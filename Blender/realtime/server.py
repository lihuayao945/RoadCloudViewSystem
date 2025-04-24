import asyncio
import websockets
import json
import csv
import os
import pymysql
from typing import Dict, List
from dataclasses import dataclass, asdict

@dataclass
class VehicleData:
    car_id: str
    color: str
    x: float
    y: float
    rot_z: float

@dataclass
class PedestrianData:
    man_id: str
    color: str
    x: float
    y: float
    rot_z: float

@dataclass
class TrafficLightData:
    light_id: str
    state: str

@dataclass
class SceneData:
    vehicles: List[VehicleData]
    pedestrians: List[PedestrianData]
    traffic_lights: List[TrafficLightData]
    update_index: int = 0

def calculate_frame_number(second: int, update_index: int) -> int:
    """计算帧号
    Args:
        second: 第几秒
        update_index: 这一秒内的第几次更新（0-2）
    Returns:
        帧号
    """
    # 每秒24帧，每个更新点间隔8帧
    base_frame = (second - 1) * 24  # 每秒24帧
    update_offset = update_index * 8  # 每次更新间隔8帧
    return base_frame + update_offset  # 从第0帧开始

# 存储所有连接的客户端
connected_clients = set()

async def broadcast(message):
    """向所有连接的客户端广播消息"""
    if connected_clients:  # 只在有客户端连接时广播
        # 创建广播任务列表
        tasks = []
        for client in connected_clients:
            try:
                # 创建发送任务
                task = asyncio.create_task(client.send(message))
                tasks.append(task)
            except Exception as e:
                print(f"创建发送任务错误: {e}")
                continue
        
        if tasks:
            try:
                # 等待所有任务完成
                await asyncio.gather(*tasks, return_exceptions=True)
            except Exception as e:
                print(f"广播消息错误: {e}")

async def process_db_data():
    """从数据库读取数据并广播到所有WebSocket客户端"""
    try:
        # 连接数据库
        connection = pymysql.connect(
            host='localhost',       # 数据库地址
            port=3306,                 # 端口号，默认是 3306
            user='root',              # 数据库用户名
            password='123456',          # 数据库密码
            database='last1',          # 数据库名
            charset='utf8mb4'          # 编码方式
        )
        
        try:
            with connection.cursor() as cursor:
                # 执行 SQL 查询
                sql = "SELECT * FROM `blender`"
                cursor.execute(sql)
                
                # 获取所有结果
                results = cursor.fetchall()
                
                # 使用字典存储每个时间戳和更新序号的数据
                data_by_timestamp = {}
                
                # 首先收集所有数据
                for row in results:
                    try:
                        # 假设数据库表结构与CSV文件结构相同
                        # 根据实际数据库表结构调整索引
                        timestamp = int(row[0])
                        update_index = int(row[1])
                        entity_type = row[2]
                        
                        # 初始化数据结构
                        if timestamp not in data_by_timestamp:
                            data_by_timestamp[timestamp] = {}
                        if update_index not in data_by_timestamp[timestamp]:
                            data_by_timestamp[timestamp][update_index] = SceneData([], [], [])
                        
                        scene_data = data_by_timestamp[timestamp][update_index]
                        
                        # 根据类型添加数据
                        if entity_type == 'vehicle':
                            vehicle = VehicleData(
                                car_id=row[3],
                                color=row[4],
                                x=float(row[5]),
                                y=float(row[6]),
                                rot_z=float(row[8])
                            )
                            scene_data.vehicles.append(vehicle)
                        elif entity_type == 'pedestrian':
                            pedestrian = PedestrianData(
                                man_id=row[3],
                                color=row[4],
                                x=float(row[5]),
                                y=float(row[6]),
                                rot_z=float(row[8])
                            )
                            scene_data.pedestrians.append(pedestrian)
                        elif entity_type == 'traffic_light':
                            traffic_light = TrafficLightData(
                                light_id=row[3],
                                state=row[4]
                            )
                            scene_data.traffic_lights.append(traffic_light)
                        
                        scene_data.update_index = update_index
                    except Exception as e:
                        print(f"处理行数据错误: {e}")
                        continue
                
                # 按时间顺序发送数据
                for timestamp in sorted(data_by_timestamp.keys()):
                    for update_index in sorted(data_by_timestamp[timestamp].keys()):
                        scene_data = data_by_timestamp[timestamp][update_index]
                        if scene_data.vehicles or scene_data.pedestrians or scene_data.traffic_lights:
                            frame_number = calculate_frame_number(timestamp, update_index)
                            
                            try:
                                # 广播完整的数据包
                                message = json.dumps({
                                    'type': 'scene_update',
                                    'data': {
                                        'vehicles': [asdict(v) for v in scene_data.vehicles],
                                        'pedestrians': [asdict(p) for p in scene_data.pedestrians],
                                        'traffic_lights': [asdict(t) for t in scene_data.traffic_lights],
                                        'update_index': update_index,
                                        'frame_number': frame_number,
                                        'timestamp': timestamp
                                    }
                                })
                                await broadcast(message)
                                # 控制发送频率，每秒3次更新
                                await asyncio.sleep(1/3)
                            except Exception as e:
                                print(f"发送数据错误: {e}")
                                return
        finally:
            connection.close()
    except Exception as e:
        print(f"数据库连接错误: {e}")
        error_message = json.dumps({
            'type': 'error',
            'message': f'数据库连接错误: {str(e)}'
        })
        await broadcast(error_message)

# 保留原有的CSV处理函数，以便需要时使用
async def process_csv_data(csv_path: str):
    """处理CSV数据并广播到所有WebSocket客户端"""
    try:
        with open(csv_path, 'r', encoding='gbk') as file:
            csv_reader = csv.reader(file)
            next(csv_reader)  # 跳过标题行
            
            # 使用字典存储每个时间戳和更新序号的数据
            data_by_timestamp = {}
            
            # 首先收集所有数据
            for row in csv_reader:
                try:
                    timestamp = int(row[0])
                    update_index = int(row[1])
                    
                    # 初始化数据结构
                    if timestamp not in data_by_timestamp:
                        data_by_timestamp[timestamp] = {}
                    if update_index not in data_by_timestamp[timestamp]:
                        data_by_timestamp[timestamp][update_index] = SceneData([], [], [])
                    
                    scene_data = data_by_timestamp[timestamp][update_index]
                    
                    # 根据类型添加数据
                    if row[2] == 'vehicle':
                        vehicle = VehicleData(
                            car_id=row[3],
                            color=row[4],
                            x=float(row[5]),
                            y=float(row[6]),
                            rot_z=float(row[8])
                        )
                        scene_data.vehicles.append(vehicle)
                    elif row[2] == 'pedestrian':
                        pedestrian = PedestrianData(
                            man_id=row[3],
                            color=row[4],
                            x=float(row[5]),
                            y=float(row[6]),
                            rot_z=float(row[8])
                        )
                        scene_data.pedestrians.append(pedestrian)
                    elif row[2] == 'traffic_light':
                        traffic_light = TrafficLightData(
                            light_id=row[3],
                            state=row[4]
                        )
                        scene_data.traffic_lights.append(traffic_light)
                    
                    scene_data.update_index = update_index
                except Exception as e:
                    print(f"处理行数据错误: {e}")
                    continue
            
            # 按时间顺序发送数据
            for timestamp in sorted(data_by_timestamp.keys()):
                for update_index in sorted(data_by_timestamp[timestamp].keys()):
                    scene_data = data_by_timestamp[timestamp][update_index]
                    if scene_data.vehicles or scene_data.pedestrians or scene_data.traffic_lights:
                        frame_number = calculate_frame_number(timestamp, update_index)
                        
                        try:
                            # 广播完整的数据包
                            message = json.dumps({
                                'type': 'scene_update',
                                'data': {
                                    'vehicles': [asdict(v) for v in scene_data.vehicles],
                                    'pedestrians': [asdict(p) for p in scene_data.pedestrians],
                                    'traffic_lights': [asdict(t) for t in scene_data.traffic_lights],
                                    'update_index': update_index,
                                    'frame_number': frame_number,
                                    'timestamp': timestamp
                                }
                            })
                            await broadcast(message)
                            # 控制发送频率，每秒3次更新
                            await asyncio.sleep(1/3)
                        except Exception as e:
                            print(f"发送数据错误: {e}")
                            return
    except Exception as e:
        print(f"读取CSV文件错误: {e}")
        error_message = json.dumps({
            'type': 'error',
            'message': f'读取CSV文件错误: {str(e)}'
        })
        await broadcast(error_message)

# 存储当前正在处理的任务
current_task = None

async def handle_websocket(websocket):
    """处理WebSocket连接"""
    global current_task
    
    try:
        print(f"新客户端连接: {websocket.remote_address}")
        # 添加到连接列表
        connected_clients.add(websocket)
        
        try:
            message = await websocket.recv()
            data = json.loads(message)
            
            if data['type'] == 'start_processing':
                # 使用数据库而不是CSV
                print("从数据库读取数据")
                
                # 如果已经有任务在运行，不要启动新的任务
                if current_task is None or current_task.done():
                    current_task = asyncio.create_task(process_db_data())
                
                # 等待任务完成
                try:
                    await current_task
                except Exception as e:
                    print(f"数据处理任务错误: {e}")
                
        except websockets.exceptions.ConnectionClosed:
            print("客户端断开连接")
        except json.JSONDecodeError:
            print("收到无效的JSON数据")
        except Exception as e:
            print(f"处理客户端消息错误: {e}")
            
    except Exception as e:
        print(f"WebSocket处理错误: {e}")
        error_message = json.dumps({
            'type': 'error',
            'message': str(e)
        })
        await broadcast(error_message)
    finally:
        # 从连接列表中移除
        connected_clients.remove(websocket)
        print(f"客户端断开连接: {websocket.remote_address}")

async def main():
    """启动WebSocket服务器"""
    try:
        server = await websockets.serve(
            handle_websocket,
            "0.0.0.0",  # 监听所有网络接口
            8765
        )
        print("WebSocket服务器已启动: ws://0.0.0.0:8765")
        await server.wait_closed()
    except Exception as e:
        print(f"启动服务器错误: {e}")

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n服务器已停止")
    except Exception as e:
        print(f"致命错误: {e}") 