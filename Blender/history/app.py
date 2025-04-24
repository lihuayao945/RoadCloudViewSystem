import sys
import bpy
import os
import math
import json
import csv
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass

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
    update_index: int = 0  # 添加更新序号属性，默认值为0

# 存储当前场景中的对象
active_objects = {
    'vehicles': {},  # car_id -> object
    'pedestrians': {},  # man_id -> object
    'traffic_lights': {}  # light_id -> object
}

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

def hide_all_objects():
    """隐藏所有车辆和行人"""
    for obj in bpy.data.objects:
        if obj.name.startswith('car') or obj.name.startswith('man'):
            obj.location.z = -5  # 修改隐藏位置为z=-5

def get_vehicle_by_id(car_id: str) -> Optional[bpy.types.Object]:
    """根据ID获取车辆对象"""
    return bpy.data.objects.get(car_id)

def get_pedestrian_by_id(man_id: str) -> Optional[bpy.types.Object]:
    """根据ID获取行人对象"""
    return bpy.data.objects.get(man_id)

def get_traffic_light_by_id(light_id: str, color: str) -> Optional[bpy.types.Object]:
    """根据ID和颜色获取交通信号灯对象"""
    # 新的ID格式为 biglights1, biglights2 等
    return bpy.data.objects.get(f"{light_id}_{color}")

def hide_inactive_objects(active_objects):
    """隐藏不在当前帧数据中的对象"""
    for obj in bpy.data.objects:
        if (obj.name.startswith('car') or obj.name.startswith('man')) and obj.name not in active_objects:
            obj.location.z = -5  # 修改隐藏位置为z=-5

def add_vehicle_animation(vehicle: bpy.types.Object, data: VehicleData, frame: int, main_action: bpy.types.Action, is_first_appearance: bool):
    """添加车辆动画"""
    # 创建动画数据
    if not vehicle.animation_data:
        vehicle.animation_data_create()
    
    # 使用传入的动作
    vehicle.animation_data.action = main_action
    
    # 保存原始旋转角度
    original_rotation = vehicle.rotation_euler.copy()
    
    # 检查对象是否刚从非活跃状态变为活跃状态
    is_reactivated = vehicle.location.z == -5
    
    # 如果是首次出现或重新激活，设置初始位置
    if is_first_appearance or is_reactivated:
        # 设置初始位置（在z=-5处）
        vehicle.location = (data.x, data.y, -5)
        vehicle.rotation_euler = (original_rotation.x, original_rotation.y, data.rot_z)
        # 插入初始位置关键帧
        vehicle.keyframe_insert(data_path="location", frame=frame-1)
        vehicle.keyframe_insert(data_path="rotation_euler", frame=frame-1)
        
        # 设置目标位置（在z=0处）
        vehicle.location = (data.x, data.y, 0)
        vehicle.rotation_euler = (original_rotation.x, original_rotation.y, data.rot_z)
        # 插入目标位置关键帧
        vehicle.keyframe_insert(data_path="location", frame=frame)
        vehicle.keyframe_insert(data_path="rotation_euler", frame=frame)
        # 设置第一帧的插值为CONSTANT（瞬时）
        for fcurve in vehicle.animation_data.action.fcurves:
            if fcurve.data_path in ["location", "rotation_euler"]:
                if fcurve.keyframe_points:
                    first_keyframe = fcurve.keyframe_points[0]
                    first_keyframe.interpolation = 'CONSTANT'
                    second_keyframe = fcurve.keyframe_points[1]
                    second_keyframe.interpolation = 'CONSTANT'
    else:
        # 设置位置和旋转
        vehicle.location = (data.x, data.y, 0)
        vehicle.rotation_euler = (original_rotation.x, original_rotation.y, data.rot_z)
        # 插入关键帧
        vehicle.keyframe_insert(data_path="location", frame=frame)
        vehicle.keyframe_insert(data_path="rotation_euler", frame=frame)
    
    # 更新材质颜色（每次调用都更新）
    for material in vehicle.data.materials:
        if material and material.name.startswith('ke'):
            # 禁用节点使用
            material.use_nodes = False
            # 根据颜色名称设置对应的RGB值
            color_map = {
                'blue': (0, 0, 1, 1),
                'red': (1, 0, 0, 1),
                'green': (0, 1, 0, 1),
                'yellow': (1, 1, 0, 1),
                'white': (1, 1, 1, 1),
                'black': (0, 0, 0, 1),
                'gray': (0.5, 0.5, 0.5, 1),
                'orange': (1, 0.5, 0, 1),
                'purple': (0.5, 0, 0.5, 1),
                'pink': (1, 0.5, 0.5, 1),
                'brown': (0.6, 0.3, 0, 1),
                'cyan': (0, 1, 1, 1),
                'magenta': (1, 0, 1, 1)
            }
            color = color_map.get(data.color.lower(), (0.5, 0.5, 0.5, 1))  # 默认灰色
            
            # 直接设置新颜色
            material.diffuse_color = color
            material.specular_intensity = 0.5
            material.roughness = 0.5
            material.metallic = 0.0
            
            # 为材质添加关键帧
            material.keyframe_insert(data_path="diffuse_color", frame=frame)
            material.keyframe_insert(data_path="specular_intensity", frame=frame)
            material.keyframe_insert(data_path="roughness", frame=frame)
            material.keyframe_insert(data_path="metallic", frame=frame)
    
    # 设置插值模式
    for fcurve in vehicle.animation_data.action.fcurves:
        for kf in fcurve.keyframe_points:
            kf.interpolation = 'LINEAR'

def add_pedestrian_animation(pedestrian: bpy.types.Object, data: PedestrianData, frame: int, main_action: bpy.types.Action, is_first_appearance: bool):
    """添加行人动画"""
    # 创建动画数据
    if not pedestrian.animation_data:
        pedestrian.animation_data_create()
    
    # 使用传入的动作
    pedestrian.animation_data.action = main_action
    
    # 保存原始旋转角度
    original_rotation = pedestrian.rotation_euler.copy()
    
    # 检查对象是否刚从非活跃状态变为活跃状态
    is_reactivated = pedestrian.location.z == -5
    
    # 如果是首次出现或重新激活，设置初始位置
    if is_first_appearance or is_reactivated:
        # 设置初始位置（在z=-5处）
        pedestrian.location = (data.x, data.y, -5)
        pedestrian.rotation_euler = (original_rotation.x, original_rotation.y, data.rot_z)
        # 插入初始位置关键帧
        pedestrian.keyframe_insert(data_path="location", frame=frame-1)
        pedestrian.keyframe_insert(data_path="rotation_euler", frame=frame-1)
        
        # 设置目标位置（在z=0处）
        pedestrian.location = (data.x, data.y, -0.3)
        pedestrian.rotation_euler = (original_rotation.x, original_rotation.y, data.rot_z)
        # 插入目标位置关键帧
        pedestrian.keyframe_insert(data_path="location", frame=frame)
        pedestrian.keyframe_insert(data_path="rotation_euler", frame=frame)
        for fcurve in pedestrian.animation_data.action.fcurves:
            if fcurve.data_path in ["location", "rotation_euler"]:
                if fcurve.keyframe_points:
                    first_keyframe = fcurve.keyframe_points[0]
                    first_keyframe.interpolation = 'CONSTANT'
                    second_keyframe = fcurve.keyframe_points[1]
                    second_keyframe.interpolation = 'CONSTANT'
    else:
        # 设置位置和旋转
        pedestrian.location = (data.x, data.y, -0.3)
        pedestrian.rotation_euler = (original_rotation.x, original_rotation.y, data.rot_z)
        # 插入关键帧
        pedestrian.keyframe_insert(data_path="location", frame=frame)
        pedestrian.keyframe_insert(data_path="rotation_euler", frame=frame)
    
    # 更新材质颜色（每次调用都更新）
    for material in pedestrian.data.materials:
        if material and material.name.startswith('body'):
            # 禁用节点使用
            material.use_nodes = False
            # 根据颜色名称设置对应的RGB值
            color_map = {
                'blue': (0, 0, 1, 1),
                'red': (1, 0, 0, 1),
                'green': (0, 1, 0, 1),
                'yellow': (1, 1, 0, 1),
                'white': (1, 1, 1, 1),
                'black': (0, 0, 0, 1),
                'gray': (0.5, 0.5, 0.5, 1),
                'orange': (1, 0.5, 0, 1),
                'purple': (0.5, 0, 0.5, 1),
                'pink': (1, 0.5, 0.5, 1),
                'brown': (0.6, 0.3, 0, 1),
                'cyan': (0, 1, 1, 1),
                'magenta': (1, 0, 1, 1)
            }
            color = color_map.get(data.color.lower(), (0.5, 0.5, 0.5, 1))  # 默认灰色
            
            # 直接设置新颜色
            material.diffuse_color = color
            material.specular_intensity = 0.5
            material.roughness = 0.5
            material.metallic = 0.0
            
            # 为材质添加关键帧
            material.keyframe_insert(data_path="diffuse_color", frame=frame)
            material.keyframe_insert(data_path="specular_intensity", frame=frame)
            material.keyframe_insert(data_path="roughness", frame=frame)
            material.keyframe_insert(data_path="metallic", frame=frame)
    
    # 设置插值模式
    for fcurve in pedestrian.animation_data.action.fcurves:
        for kf in fcurve.keyframe_points:
            kf.interpolation = 'LINEAR'

def add_traffic_light_animation(traffic_light, color, frame, action, last_color=None):
    """为交通信号灯添加动画
    Args:
        traffic_light: 信号灯对象
        color: 当前颜色
        frame: 当前帧
        action: 动作对象
        last_color: 上一次的颜色，如果为None表示首次出现
    """
    # 创建动画数据
    if not traffic_light.animation_data:
        traffic_light.animation_data_create()
    
    # 使用传入的动作
    traffic_light.animation_data.action = action
    
    # 获取信号灯ID
    light_id = traffic_light.name.split('_')[0]
    
    # 根据不同的light_id设置不同的正常大小
    if light_id in ['biglights1', 'biglights2']:
        normal_scale = (1.136, 1.803, 2.405)
    elif light_id in ['biglights3', 'biglights4']:
        normal_scale = (1.212, 2.865, 2.840)
    else:
        normal_scale = (0.019, 0.019, 0.019)
    
    # 几乎不可见的大小
    hidden_scale = (0.001, 0.001, 0.001)
    
    # 如果颜色没有变化，保持当前状态
    if last_color == color:
        # 保持当前状态
        traffic_light.scale = normal_scale
        traffic_light.keyframe_insert(data_path="scale", frame=frame)
        return
    
    # 颜色发生变化，执行切换动画
    if color == 'green':
        # 绿灯动画
        # 前一帧（如果是红灯）
        red_light = get_traffic_light_by_id(light_id, 'red')
        if red_light:
            red_light.scale = normal_scale
            red_light.keyframe_insert(data_path="scale", frame=frame-1)
            red_light.scale = hidden_scale
            red_light.keyframe_insert(data_path="scale", frame=frame)
        
        # 当前帧（绿灯）
        traffic_light.scale = hidden_scale
        traffic_light.keyframe_insert(data_path="scale", frame=frame-1)
        traffic_light.scale = normal_scale
        traffic_light.keyframe_insert(data_path="scale", frame=frame)
        
    elif color == 'yellow':
        # 黄灯动画
        # 前一帧（如果是绿灯）
        green_light = get_traffic_light_by_id(light_id, 'green')
        if green_light:
            green_light.scale = normal_scale
            green_light.keyframe_insert(data_path="scale", frame=frame-1)
            green_light.scale = hidden_scale
            green_light.keyframe_insert(data_path="scale", frame=frame)
        
        # 当前帧（黄灯）
        traffic_light.scale = hidden_scale
        traffic_light.keyframe_insert(data_path="scale", frame=frame-1)
        traffic_light.scale = normal_scale
        traffic_light.keyframe_insert(data_path="scale", frame=frame)
        
    elif color == 'red':
        # 红灯动画
        # 前一帧（如果是黄灯）
        yellow_light = get_traffic_light_by_id(light_id, 'yellow')
        if yellow_light:
            yellow_light.scale = normal_scale
            yellow_light.keyframe_insert(data_path="scale", frame=frame-1)
            yellow_light.scale = hidden_scale
            yellow_light.keyframe_insert(data_path="scale", frame=frame)
        
        # 当前帧（红灯）
        traffic_light.scale = hidden_scale
        traffic_light.keyframe_insert(data_path="scale", frame=frame-1)
        traffic_light.scale = normal_scale
        traffic_light.keyframe_insert(data_path="scale", frame=frame)
    
    # 设置插值模式为常量，确保切换是瞬间的
    for fcurve in traffic_light.animation_data.action.fcurves:
        for kf in fcurve.keyframe_points:
            kf.interpolation = 'CONSTANT'

def get_available_vehicle() -> Optional[bpy.types.Object]:
    """从对象池中获取一个可用的车辆对象"""
    for obj in bpy.data.objects:
        if obj.name.startswith('car') and obj.location.z == -5:
            return obj
    return None

def get_available_pedestrian() -> Optional[bpy.types.Object]:
    """从对象池中获取一个可用的行人对象"""
    for obj in bpy.data.objects:
        if obj.name.startswith('man') and obj.location.z == -5:
            return obj
    return None

def process_scene_data(scene_data: List[SceneData], main_action: bpy.types.Action):
    """处理场景数据"""
    # 存储当前活跃的对象映射 {对象ID: 对象}
    active_objects = {}
    # 存储信号灯上一次的颜色状态
    last_traffic_light_colors = {}
    # 存储对象上一次的颜色状态
    last_object_colors = {}
    
    # 隐藏所有对象
    hide_all_objects()
    
    # 处理每一帧的数据
    for second, data in enumerate(scene_data, 1):
        # 更新当前帧的活跃对象映射
        current_active_objects = {}
        
        # 使用calculate_frame_number函数计算帧号
        frame = calculate_frame_number(second, data.update_index)
        
        # 处理车辆
        for vehicle_data in data.vehicles:
            # 如果车辆已经在活跃列表中，直接使用
            if vehicle_data.car_id in active_objects:
                vehicle = active_objects[vehicle_data.car_id]
                # 设置位置和动画
                add_vehicle_animation(vehicle, vehicle_data, frame, main_action, False)
            else:
                # 否则从对象池中获取一个可用的车辆
                vehicle = get_available_vehicle()
                if not vehicle:
                    print(f"警告：没有可用的车辆对象用于 {vehicle_data.car_id}")
                    continue
                # 将车辆添加到活跃列表中
                active_objects[vehicle_data.car_id] = vehicle
                # 设置位置和动画
                add_vehicle_animation(vehicle, vehicle_data, frame, main_action, True)
            
            current_active_objects[vehicle_data.car_id] = vehicle
            # 记录当前颜色
            last_object_colors[vehicle_data.car_id] = vehicle_data.color
        
        # 处理行人
        for pedestrian_data in data.pedestrians:
            # 如果行人已经在活跃列表中，直接使用
            if pedestrian_data.man_id in active_objects:
                pedestrian = active_objects[pedestrian_data.man_id]
                # 设置位置和动画
                add_pedestrian_animation(pedestrian, pedestrian_data, frame, main_action, False)
            else:
                # 否则从对象池中获取一个可用的行人
                pedestrian = get_available_pedestrian()
                if not pedestrian:
                    print(f"警告：没有可用的行人对象用于 {pedestrian_data.man_id}")
                    continue
                # 将行人添加到活跃列表中
                active_objects[pedestrian_data.man_id] = pedestrian
                # 设置位置和动画
                add_pedestrian_animation(pedestrian, pedestrian_data, frame, main_action, True)
            
            current_active_objects[pedestrian_data.man_id] = pedestrian
            # 记录当前颜色
            last_object_colors[pedestrian_data.man_id] = pedestrian_data.color
        
        # 处理交通信号灯
        for traffic_light_data in data.traffic_lights:
            traffic_light = get_traffic_light_by_id(traffic_light_data.light_id, traffic_light_data.state)
            
            if traffic_light:
                # 获取上一次的颜色状态
                last_color = last_traffic_light_colors.get(traffic_light_data.light_id)
                # 添加动画
                add_traffic_light_animation(traffic_light, traffic_light_data.state, frame, main_action, last_color)
                # 更新颜色状态
                last_traffic_light_colors[traffic_light_data.light_id] = traffic_light_data.state
        
        # 处理不再活跃的对象
        inactive_objects = []
        for obj_id, obj in active_objects.items():
            if obj_id not in current_active_objects:
                # 在当前位置和旋转角度插入关键帧
                obj.keyframe_insert(data_path="location", frame=frame-1)
                obj.keyframe_insert(data_path="rotation_euler", frame=frame-1)
                
                # 保持当前颜色直到消失前
                if obj.name.startswith('car'):
                    for material in obj.data.materials:
                        if material and material.name.startswith('ke'):
                            # 使用上一次记录的颜色
                            last_color = last_object_colors.get(obj_id, 'gray')
                            color_map = {
                                'blue': (0, 0, 1, 1),
                                'red': (1, 0, 0, 1),
                                'green': (0, 1, 0, 1),
                                'yellow': (1, 1, 0, 1),
                                'white': (1, 1, 1, 1),
                                'black': (0, 0, 0, 1),
                                'gray': (0.5, 0.5, 0.5, 1),
                                'orange': (1, 0.5, 0, 1),
                                'purple': (0.5, 0, 0.5, 1),
                                'pink': (1, 0.5, 0.5, 1),
                                'brown': (0.6, 0.3, 0, 1),
                                'cyan': (0, 1, 1, 1),
                                'magenta': (1, 0, 1, 1)
                            }
                            color = color_map.get(last_color.lower(), (0.5, 0.5, 0.5, 1))
                            material.diffuse_color = color
                            material.keyframe_insert(data_path="diffuse_color", frame=frame)
                elif obj.name.startswith('man'):
                    for material in obj.data.materials:
                        if material and material.name.startswith('body'):
                            # 使用上一次记录的颜色
                            last_color = last_object_colors.get(obj_id, 'gray')
                            color_map = {
                                'blue': (0, 0, 1, 1),
                                'red': (1, 0, 0, 1),
                                'green': (0, 1, 0, 1),
                                'yellow': (1, 1, 0, 1),
                                'white': (1, 1, 1, 1),
                                'black': (0, 0, 0, 1),
                                'gray': (0.5, 0.5, 0.5, 1),
                                'orange': (1, 0.5, 0, 1),
                                'purple': (0.5, 0, 0.5, 1),
                                'pink': (1, 0.5, 0.5, 1),
                                'brown': (0.6, 0.3, 0, 1),
                                'cyan': (0, 1, 1, 1),
                                'magenta': (1, 0, 1, 1)
                            }
                            color = color_map.get(last_color.lower(), (0.5, 0.5, 0.5, 1))
                            material.diffuse_color = color
                            material.keyframe_insert(data_path="diffuse_color", frame=frame)
                
                # 瞬间移动到z=-5
                obj.location = (0, 0, -5)
                obj.keyframe_insert(data_path="location", frame=frame)
                obj.keyframe_insert(data_path="rotation_euler", frame=frame)
                # 设置关键帧插值为CONSTANT，确保瞬间移动
                for fcurve in obj.animation_data.action.fcurves:
                    if fcurve.data_path in ["location", "rotation_euler"]:
                        if fcurve.keyframe_points:
                            last_keyframe = fcurve.keyframe_points[-1]
                            last_keyframe.interpolation = 'CONSTANT'
                # 记录需要移除的对象ID
                inactive_objects.append(obj_id)
        
        # 从活跃列表中移除不活跃的对象
        for obj_id in inactive_objects:
            del active_objects[obj_id]
            if obj_id in last_object_colors:
                del last_object_colors[obj_id]
        
        # 更新活跃对象映射
        active_objects = current_active_objects
    
    # 在动画结束时将所有对象放回隐藏位置
    final_frame = len(scene_data) * 24
    for obj in bpy.data.objects:
        if obj.name.startswith('car') or obj.name.startswith('man'):
            # 在当前位置插入关键帧
            obj.keyframe_insert(data_path="location", frame=final_frame-1)
            # 移动到隐藏位置
            obj.location = (0, 0, -5)
            obj.keyframe_insert(data_path="location", frame=final_frame)
            # 设置关键帧插值为CONSTANT，确保瞬间移动
            for fcurve in obj.animation_data.action.fcurves:
                # 只修改最后一个关键帧的插值
                if fcurve.keyframe_points:
                    last_keyframe = fcurve.keyframe_points[-1]
                    last_keyframe.interpolation = 'CONSTANT'

def read_scene_data_from_csv(csv_path: str) -> List[SceneData]:
    """从CSV文件读取场景数据"""
    scene_data = []
    current_second = -1
    current_data = None
    
    with open(csv_path, 'r', encoding='gbk') as f:
        content = f.read()
    
    from io import StringIO
    f = StringIO(content)
    reader = csv.DictReader(f)
    
    for row in reader:
        try:
            second = int(row['times'])
            update_index = int(row['series'])  # 读取更新序号
            
            # 如果秒数变化，创建新的场景数据
            if second != current_second:
                if current_data:
                    scene_data.append(current_data)
                current_second = second
                current_data = SceneData(vehicles=[], pedestrians=[], traffic_lights=[], update_index=0)  # 初始化更新序号为0
                print(f"处理第 {second} 秒的数据")
            
            # 更新当前场景数据的更新序号
            current_data.update_index = update_index
            
            if row['type'] == 'vehicle':
                current_data.vehicles.append(VehicleData(
                    car_id=row['obj_id'],
                    color=row['color'],
                    x=float(row['x']),
                    y=float(row['y']),
                    rot_z=float(row['angle'])
                ))
            elif row['type'] == 'pedestrian':
                current_data.pedestrians.append(PedestrianData(
                    man_id=row['obj_id'],
                    color=row['color'],
                    x=float(row['x']),
                    y=float(row['y']),
                    rot_z=float(row['angle'])
                ))
            elif row['type'] == 'traffic_light':
                current_data.traffic_lights.append(TrafficLightData(
                    light_id=row['obj_id'],
                    state=row['color']
                ))
        except Exception as e:
            print(f"处理数据时出错: {e}")
            print(f"当前行数据: {row}")
            raise
    
    # 添加最后一个时间点的数据
    if current_data:
        scene_data.append(current_data)
    
    print(f"总共处理了 {len(scene_data)} 个时间点的数据")
    return scene_data

def initialize_object_pool():
    """初始化对象池，确保所有车辆和行人对象都在正确的位置"""
    # 统计车辆和行人对象数量
    vehicle_count = 0
    pedestrian_count = 0
    
    for obj in bpy.data.objects:
        if obj.name.startswith('car'):
            vehicle_count += 1
            obj.location.z = -5  # 确保所有车辆都在对象池中
        elif obj.name.startswith('man'):
            pedestrian_count += 1
            obj.location.z = -5  # 确保所有行人都在对象池中
    
    print(f"对象池初始化完成：{vehicle_count} 辆车辆，{pedestrian_count} 个行人")

def main(csv_path,filepath):
    # 清除场景
    bpy.ops.wm.read_factory_settings(use_empty=True)

    # 导入模型
    model_path = "scene_longhu.fbx"
    if os.path.exists(model_path):
        car = bpy.ops.import_scene.fbx(filepath=model_path)
        if car:
            # 创建一个主动作
            main_action = bpy.data.actions.new(name="Main_action")
            
            # 设置场景帧率
            bpy.context.scene.render.fps = 24
            
            # 初始化对象池
            initialize_object_pool()
            
            # 从CSV文件读取数据
            #csv_path = "motion_data.csv"  # 使用更新后的CSV文件
            if os.path.exists(csv_path):
                scene_data = read_scene_data_from_csv(csv_path)
                
                # 处理场景数据
                process_scene_data(scene_data, main_action)
                
                # 设置场景帧范围
                bpy.context.scene.frame_start = 1
                # 计算总帧数：每秒24帧
                total_frames = len(scene_data) * 24  # 每秒24帧
                bpy.context.scene.frame_end = total_frames
                
                # 保存场景
                #bpy.ops.wm.save_as_mainfile(filepath="tryagainandagain.blend")
                # export_scene_to_fbx()
                # 导出glTF
                export_scene_to_gltf(filepath)
            else:
                print(f"错误：找不到CSV数据文件 {csv_path}")
    else:
        print(f"错误：找不到模型文件 {model_path}")

def export_scene_to_fbx():
    """导出场景为FBX格式"""
    # 确保所有对象可见
    for obj in bpy.data.objects:
        obj.hide_viewport = False
        obj.hide_render = False
    # 选择所有对象
    bpy.ops.object.select_all(action='SELECT')
    # 设置导出选项
    bpy.ops.export_scene.fbx(
        filepath="car_animation.fbx",
        use_selection=True,
        use_mesh_modifiers=True,
        path_mode='COPY',
        embed_textures=True,
        use_mesh_edges=True,
        use_tspace=True,
        use_custom_props=True,
        mesh_smooth_type='FACE',
        use_subsurf=False,
        axis_forward='-Z',
        axis_up='Y',
        # 动画相关设置
        bake_anim=True,
        bake_anim_step=1.0,
        bake_anim_simplify_factor=1.0,
        bake_anim_use_all_bones=True,
        bake_anim_force_startend_keying=True,
        # 材质和网格设置
        use_metadata=True
    )
    print(f"场景已导出为: car_animation.fbx")

def export_scene_to_gltf(savefilepath):
    """导出场景为glTF格式，确保高精度"""
    # 选择所有对象
    bpy.ops.object.select_all(action='SELECT')
    #filepath = "output/" + savefilepath
    # 设置导出选项
    bpy.ops.export_scene.gltf(
        #filepath="tryagainandagain.glb",  # 文件路径
        filepath="output/" + savefilepath,
        export_format='GLB',  # 导出格式为 GLB
        export_apply=True,  # 应用所有变换（位置、旋转、缩放）
        export_animations=True,  # 导出动画
        export_skins=True,  # 导出骨骼绑定数据
        export_yup=True,  # 使用右手坐标系（确保与其他工具兼容）
        export_cameras=False,  # 不导出摄像机
        export_lights=False,  # 不导出光源
        export_texcoords=True,  # 导出纹理坐标
        export_normals=True,  # 导出法线数据
        export_tangents=True,  # 导出切线数据
        export_materials='EXPORT',  # 导出材质
        export_image_format='AUTO',  # 图片纹理的格式
        export_extras=True  # 导出附加数据
    )

    print(f"场景已导出为: " + "output/" + savefilepath)
if __name__ == "__main__":
    # 从命令行参数获取CSV文件路径和保存路径
    # 跳过Blender的内部参数，获取参数
    input_path = None
    output_path = None
    if len(sys.argv) > 2:
        # 获取倒数第二个参数作为CSV文件路径
        input_path = sys.argv[-2]
        # 获取最后一个参数作为保存路径
        output_path = sys.argv[-1]
        # 检查倒数第二个参数是否是.csv文件
        if not input_path.endswith('.csv'):
            input_path = None
    if input_path and output_path:
        print(f"使用命令行参数提供的CSV文件路径: {input_path}")
        print(f"使用命令行参数提供的保存路径: {output_path}")
        main(input_path, output_path)
    else:
        # 如果没有找到有效的参数，则请求用户输入
        input_path = input("请输入CSV文件路径: ")
        output_path = input("请输入保存路径: ")
        main(input_path, output_path)
