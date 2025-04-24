import time
from flask import Flask, jsonify, request, redirect, send_from_directory, url_for, render_template_string
import os

app = Flask(__name__)

# 上传文件保存路径
UPLOAD_FOLDER = 'uploads'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

# 允许上传的文件扩展名（可根据需要自定义）
ALLOWED_EXTENSIONS = {'txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif', 'zip','csv'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# 上传页面
HTML_TEMPLATE = '''
<!doctype html>
<title>上传文件</title>
<h1>上传文件</h1>
<form method=post enctype=multipart/form-data>
  <input type=file name=file>
  <input type=submit value=上传>
</form>
'''

@app.route('/', methods=['GET', 'POST'])
def upload_file():
    if request.method == 'POST':
        # 检查是否有文件部分
        if 'file' not in request.files:
            return '没有文件 part'
        file = request.files['file']
        if file.filename == '':
            return '未选择文件'
        if file and allowed_file(file.filename):
            filename = file.filename
            file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
            
            # 如果上传的是CSV文件，则调用Blender处理
            if filename.endswith('.csv'):
                # 构建输出文件名
                output_filename = str( int(time.time()) ) + ".glb"
                # 构建Blender命令
                csv_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
                # Dev环境
                blender_command = f'"D:/Program Files (x86)/Blender 4.4/blender.exe" --background --python app.py -- "{csv_path}" "{output_filename}"'
                print(output_filename)
                # Prod环境
                #blender_command = f'blender --background --python app.py -- {csv_path} {output_filename}'
                # 执行系统命令
                import subprocess
                try:
                    subprocess.run(blender_command, shell=True, check=True)
                    download_url = url_for('download_file', filename=output_filename)
                    # 构建成功响应的JSON数据
                    response_data = {
                        "status": "success",
                        "url": download_url
                    }
                    # 返回JSON响应
                    return jsonify(response_data)
                    
                except subprocess.CalledProcessError as e:
                    response_data = {
                        "status": "fail",
                        "msg": "转换失败！"
                    }
                    # 返回JSON响应
                    return jsonify(response_data)



            return f'文件 {filename} 上传成功！'
    return render_template_string(HTML_TEMPLATE)

@app.route('/download/<filename>')
def download_file(filename):
    return send_from_directory("output", filename, as_attachment=True)


if __name__ == '__main__':
    app.run(host='localhost', port=5147, debug=True)
