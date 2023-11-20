import colorsys
import os

import cv2
from flask import Flask, request, jsonify, send_file
from flask_restx import Resource, Api
from werkzeug.utils import secure_filename
from werkzeug.serving import WSGIRequestHandler
import urllib3
import json
import base64
import re
import numpy as np
import random
import uuid

WSGIRequestHandler.protocol_version = "HTTP/1.1"

app = Flask(__name__)
api = Api(app)

UPLOAD_FOLDER = 'upload'
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

EDIT_FOLDER = 'edit'
app.config['EDIT_FOLDER'] = EDIT_FOLDER

CROP_FOLDER = 'crop'
app.config['CROP_FOLDER'] = CROP_FOLDER

color_data = {
    "tone_in_tone": (0, 0, 0),
    "tone_in_tone_1": (0, 0, 0,),
    "tone_in_tone_2": (0, 0, 0),
    "tone_on_tone": (0, 0, 0),
    "url": ""
} #컬러 json 파일

pants_data = {
    "mask" : [0,0]
}

person_position = {
    "pos" : [0,0,0,0]
}


class RGBColor: # api한테 받은 상의 색 정의
    def __init__(self, red, green, blue):
        self.red = red
        self.green = green
        self.blue = blue


def linear_interpolation(color1, color2, ratio):
    r = int(color1.red * (1 - ratio) + color2.red * ratio)
    g = int(color1.green * (1 - ratio) + color2.green * ratio)
    b = int(color1.blue * (1 - ratio) + color2.blue * ratio)
    return RGBColor(r, g, b)


def generate_tone_on_tone_colors(base_color):
    tone_on_tone_colors = []

    for i in range(1, 7):  # 0.0과 1.0을 제외한 범위
        ratio = i / 10.0
        tone_color = linear_interpolation(base_color, RGBColor(255, 255, 255), ratio)
        tone_on_tone_colors.append(tone_color)

    return tone_on_tone_colors


def calculate_brightness(color):
    # Use YUV color space formula to calculate brightness
    return 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue

def generate_tone_in_tone_color(base_color, num_colors=10, brightness_threshold=200):
    recommended_colors = []

    for _ in range(num_colors):
        while True:
            random_color = RGBColor(np.random.randint(0, 256), np.random.randint(0, 256), np.random.randint(0, 256))
            # Check if the brightness of the random color is below the threshold
            if calculate_brightness(random_color) < brightness_threshold:
                recommended_colors.append(random_color)
                break

    return recommended_colors

# 전역 변수로 file_name을 선언하고 초기화합니다
file_name = None

@api.route('/upload')
class Uploading(Resource):
    def post(self):
        global file_name  # file_name 변수를 전역 변수로 사용
        is_skirt_checked = request.form.get('isSkirtChecked')
        is_coat_checked = request.form.get('isCoatChecked')
        is_pants_checked = request.form.get('isPantsChecked')
        print(is_skirt_checked, is_coat_checked,is_pants_checked)
        f = request.files['files'] #파일 요청
        if f:
            # 이미지를 업로드할 디렉토리를 생성하고 이미지를 저장
            upload_dir = os.path.join(app.config['UPLOAD_FOLDER'], secure_filename(f.filename))
            f.save(upload_dir)

            file_name = f.filename

            # 여기에 두 번째 코드 조각을 추가
            openApiURL = "http://aiopen.etri.re.kr:8000/HumanParsing"
            accessKey = "b0bff3a7-46f6-45c7-b5b8-d4d8f3004e6a"
            imageFilePath = upload_dir  # 업로드된 이미지 파일 경로
            type = "jpg"

            file = open(imageFilePath, "rb")
            imageContents = base64.b64encode(file.read()).decode("utf8")
            file.close()

            requestJson = {
                "argument": {
                    "type": type,
                    "file": imageContents
                }
            }

            http = urllib3.PoolManager()
            response = http.request(
                "POST",
                openApiURL,
                headers={"Content-Type": "application/json; charset=UTF-8", "Authorization": accessKey},
                body=json.dumps(requestJson)
            )

            print("[responseCode] " + str(response.status)) #200이 뜨면 api 서버 요청이 됐다는 뜻

            jsonObject = json.loads(response.data) #response.data는 api가 준 json 데이터임
            if is_skirt_checked == 'true' and is_pants_checked == 'false':
                data = jsonObject['return_object']['person_1']['skirt color']
                numeric_string = data.replace("(", "").replace(")", "").replace(",", "") #data가 String이기 때문에 형 변환 과정
                number_strings = numeric_string.split()
                numbers = [int(num) for num in number_strings]
                base_color = RGBColor(numbers[0],numbers[1],numbers[2])  # 여기에 형 변환이 완료된 rgb값 배열 하나하나씩 넣기
                tone_on_tone_colors = generate_tone_on_tone_colors(base_color)
                random_tot_color = random.choice(tone_on_tone_colors)
                num_choices = 3
                tit_color = generate_tone_in_tone_color(base_color, num_colors=10)
                # 중복 없이 num_choices 만큼의 색상을 무작위로 선택
                random_choices = random.sample(tit_color, num_choices)
                random_tit_color = random_choices[0]
                random_tit_color_1 = random_choices[1]
                random_tit_color_2 = random_choices[2]
                color_data["tone_on_tone"] = (random_tot_color .red, random_tot_color .green, random_tot_color .blue)  # 위에서 돌린 색을 넣어줌
                color_data["tone_in_tone"] = (random_tit_color.red, random_tit_color.green, random_tit_color.blue)
                color_data["tone_in_tone_1"] = (random_tit_color_1.red, random_tit_color_1.green, random_tit_color_1.blue)
                color_data["tone_in_tone_2"] = (random_tit_color_2.red, random_tit_color_2.green, random_tit_color_2.blue)

            elif is_skirt_checked== 'false' and is_pants_checked=='false':
                data = jsonObject['return_object']['person_1']['pants color']
                numeric_string = data.replace("(", "").replace(")", "").replace(",", "") #data가 String이기 때문에 형 변환 과정
                number_strings = numeric_string.split()
                numbers = [int(num) for num in number_strings]
                base_color = RGBColor(numbers[0],numbers[1],numbers[2])  # 여기에 형 변환이 완료된 rgb값 배열 하나하나씩 넣기
                tone_on_tone_colors = generate_tone_on_tone_colors(base_color)
                random_tot_color = random.choice(tone_on_tone_colors)
                num_choices = 3
                tit_color = generate_tone_in_tone_color(base_color, num_colors=10)
                # 중복 없이 num_choices 만큼의 색상을 무작위로 선택
                random_choices = random.sample(tit_color, num_choices)
                random_tit_color = random_choices[0]
                random_tit_color_1 = random_choices[1]
                random_tit_color_2 = random_choices[2]
                color_data["tone_on_tone"] = (random_tot_color .red, random_tot_color .green, random_tot_color .blue)  # 위에서 돌린 색을 넣어줌
                color_data["tone_in_tone"] = (random_tit_color.red, random_tit_color.green, random_tit_color.blue)
                color_data["tone_in_tone_1"] = (random_tit_color_1.red, random_tit_color_1.green, random_tit_color_1.blue)
                color_data["tone_in_tone_2"] = (random_tit_color_2.red, random_tit_color_2.green, random_tit_color_2.blue)
            
            if is_pants_checked == 'true' and is_skirt_checked=='false':
                data = jsonObject['return_object']['person_1']['upcloth color']
                numeric_string = data.replace("(", "").replace(")", "").replace(",", "") #data가 String이기 때문에 형 변환 과정
                number_strings = numeric_string.split()
                numbers = [int(num) for num in number_strings]
                base_color = RGBColor(numbers[0],numbers[1],numbers[2])  # 여기에 형 변환이 완료된 rgb값 배열 하나하나씩 넣기
                tone_on_tone_colors = generate_tone_on_tone_colors(base_color)
                random_tot_color = random.choice(tone_on_tone_colors)
                num_choices = 3
                tit_color = generate_tone_in_tone_color(base_color, num_colors=10)
                # 중복 없이 num_choices 만큼의 색상을 무작위로 선택
                random_choices = random.sample(tit_color, num_choices)
                random_tit_color = random_choices[0]
                random_tit_color_1 = random_choices[1]
                random_tit_color_2 = random_choices[2]
                color_data["tone_on_tone"] = (random_tot_color .red, random_tot_color .green, random_tot_color .blue)  # 위에서 돌린 색을 넣어줌
                color_data["tone_in_tone"] = (random_tit_color.red, random_tit_color.green, random_tit_color.blue)
                color_data["tone_in_tone_1"] = (random_tit_color_1.red, random_tit_color_1.green, random_tit_color_1.blue)
                color_data["tone_in_tone_2"] = (random_tit_color_2.red, random_tit_color_2.green, random_tit_color_2.blue)
            elif is_pants_checked=='false' and is_skirt_checked=='false':
                data = jsonObject['return_object']['person_1']['upcloth color']
                numeric_string = data.replace("(", "").replace(")", "").replace(",", "") #data가 String이기 때문에 형 변환 과정
                number_strings = numeric_string.split()
                numbers = [int(num) for num in number_strings]
                base_color = RGBColor(numbers[0],numbers[1],numbers[2])  # 여기에 형 변환이 완료된 rgb값 배열 하나하나씩 넣기
                tone_on_tone_colors = generate_tone_on_tone_colors(base_color)
                random_tot_color = random.choice(tone_on_tone_colors)
                num_choices = 3
                tit_color = generate_tone_in_tone_color(base_color, num_colors=10)
                # 중복 없이 num_choices 만큼의 색상을 무작위로 선택
                random_choices = random.sample(tit_color, num_choices)
                random_tit_color = random_choices[0]
                random_tit_color_1 = random_choices[1]
                random_tit_color_2 = random_choices[2]
                color_data["tone_on_tone"] = (random_tot_color .red, random_tot_color .green, random_tot_color .blue)  # 위에서 돌린 색을 넣어줌
                color_data["tone_in_tone"] = (random_tit_color.red, random_tit_color.green, random_tit_color.blue)
                color_data["tone_in_tone_1"] = (random_tit_color_1.red, random_tit_color_1.green, random_tit_color_1.blue)
                color_data["tone_in_tone_2"] = (random_tit_color_2.red, random_tit_color_2.green, random_tit_color_2.blue)
            

            #아우터 여부에 따른 체크박스 값
            if is_coat_checked == 'true' and is_pants_checked=='false':
                mask = jsonObject['return_object']['person_1']['coat mask']
                pants_color = jsonObject['return_object']['person_1']['pants color']
                pants_colors = [int(x) for x in pants_color.strip('()').split(',')]
                color_data["pants_color"] = (pants_colors[2], pants_colors[1], pants_colors[0])
                pos = jsonObject['return_object']['person_1']['position']

                pants_data["mask"] = mask
                person_position["pos"] = pos


                image = cv2.imread(upload_dir)

                coordinates = mask
                # 문자열에서 공백을 제거하고 "[x, y]" 형식으로 수정
                formatted_coordinates = re.sub(r'\[(\s+)', '[', coordinates)
                formatted_coordinates = re.sub(r'(\s+)\]', ']', formatted_coordinates)
                formatted_coordinates = re.sub(r'\s+', ', ', formatted_coordinates)

                contour_points = [list(map(int, point.strip('[]').split(','))) for point in formatted_coordinates.split('], [')]
                input_string = pos
                # 문자열에서 괄호와 공백을 제거합니다.
                cleaned_string = input_string.replace("[", "").replace("]", "").replace(" ", "")
                # 쉼표로 문자열을 분할하여 숫자를 얻습니다.
                numbers = cleaned_string.split(",")
                # 문자열을 정수로 변환합니다.
                result_array = [int(number) for number in numbers]
                y1 = result_array[0] # 왼쪽 상단 y
                x1 = result_array[1] # 왼쪽 상단 x

                contour_points += np.array([int(x1),int(y1)])

                image_with_contour = image.copy()


                crop_1 = np.zeros_like(image)
                crop_2 = np.zeros_like(image)
                crop_3 = np.zeros_like(image)
                crop_4 = np.zeros_like(image)

                crop_1 = cv2.resize(crop_1,(image.shape[1],image.shape[0]))
                crop_2 = cv2.resize(crop_2,(image.shape[1],image.shape[0]))
                crop_3 = cv2.resize(crop_3,(image.shape[1],image.shape[0]))
                crop_4 = cv2.resize(crop_4,(image.shape[1],image.shape[0]))


                cv2.drawContours(image_with_contour, [np.array(contour_points)], 0, (random_tit_color.blue, random_tit_color.green,random_tit_color.red), thickness=cv2.FILLED)

                cv2.drawContours(crop_1, [np.array(contour_points)], 0, (random_tit_color.blue, random_tit_color.green,random_tit_color.red), thickness=cv2.FILLED)
                cv2.drawContours(crop_2, [np.array(contour_points)], 0, (random_tit_color_1.blue, random_tit_color_1.green,random_tit_color_1.red), thickness=cv2.FILLED)
                cv2.drawContours(crop_3, [np.array(contour_points)], 0, (random_tit_color_2.blue, random_tit_color_2.green,random_tit_color_2.red), thickness=cv2.FILLED)
                cv2.drawContours(crop_4, [np.array(contour_points)], 0, (random_tot_color.blue, random_tot_color.green,random_tot_color.red), thickness=cv2.FILLED)

                #contour좌표로 덮은 하의 색
                crop_dir = os.path.join(app.config['CROP_FOLDER'])
                cv2.imwrite(os.path.join(crop_dir,'tit_1.jpg'),crop_1)
                cv2.imwrite(os.path.join(crop_dir,'tit_2.jpg'),crop_2)
                cv2.imwrite(os.path.join(crop_dir,'tit_3.jpg'),crop_3)
                cv2.imwrite(os.path.join(crop_dir,'tot_1.jpg'),crop_4)

                result = cv2.addWeighted(image, 0.7, crop_1, 0.5, 0)



                unique_filename = str(uuid.uuid4()) + '.jpg'

                image_url = unique_filename

                color_data["url"] = image_url

                edit_dir = os.path.join(app.config['EDIT_FOLDER'], unique_filename)
                cv2.imwrite(edit_dir, result)
            elif is_coat_checked=='false' and is_pants_checked=='false':
                mask = jsonObject['return_object']['person_1']['upcloth mask']
                pants_color = jsonObject['return_object']['person_1']['pants color']
                pants_colors = [int(x) for x in pants_color.strip('()').split(',')]
                color_data["pants_color"] = (pants_colors[2], pants_colors[1], pants_colors[0])
                pos = jsonObject['return_object']['person_1']['position']

                pants_data["mask"] = mask
                person_position["pos"] = pos


                image = cv2.imread(upload_dir)

                coordinates = mask
                # 문자열에서 공백을 제거하고 "[x, y]" 형식으로 수정
                formatted_coordinates = re.sub(r'\[(\s+)', '[', coordinates)
                formatted_coordinates = re.sub(r'(\s+)\]', ']', formatted_coordinates)
                formatted_coordinates = re.sub(r'\s+', ', ', formatted_coordinates)

                contour_points = [list(map(int, point.strip('[]').split(','))) for point in formatted_coordinates.split('], [')]
                input_string = pos
                # 문자열에서 괄호와 공백을 제거합니다.
                cleaned_string = input_string.replace("[", "").replace("]", "").replace(" ", "")
                # 쉼표로 문자열을 분할하여 숫자를 얻습니다.
                numbers = cleaned_string.split(",")
                # 문자열을 정수로 변환합니다.
                result_array = [int(number) for number in numbers]
                y1 = result_array[0] # 왼쪽 상단 y
                x1 = result_array[1] # 왼쪽 상단 x

                contour_points += np.array([int(x1),int(y1)])

                image_with_contour = image.copy()


                crop_1 = np.zeros_like(image)
                crop_2 = np.zeros_like(image)
                crop_3 = np.zeros_like(image)
                crop_4 = np.zeros_like(image)

                crop_1 = cv2.resize(crop_1,(image.shape[1],image.shape[0]))
                crop_2 = cv2.resize(crop_2,(image.shape[1],image.shape[0]))
                crop_3 = cv2.resize(crop_3,(image.shape[1],image.shape[0]))
                crop_4 = cv2.resize(crop_4,(image.shape[1],image.shape[0]))


                cv2.drawContours(image_with_contour, [np.array(contour_points)], 0, (random_tit_color.blue, random_tit_color.green,random_tit_color.red), thickness=cv2.FILLED)

                cv2.drawContours(crop_1, [np.array(contour_points)], 0, (random_tit_color.blue, random_tit_color.green,random_tit_color.red), thickness=cv2.FILLED)
                cv2.drawContours(crop_2, [np.array(contour_points)], 0, (random_tit_color_1.blue, random_tit_color_1.green,random_tit_color_1.red), thickness=cv2.FILLED)
                cv2.drawContours(crop_3, [np.array(contour_points)], 0, (random_tit_color_2.blue, random_tit_color_2.green,random_tit_color_2.red), thickness=cv2.FILLED)
                cv2.drawContours(crop_4, [np.array(contour_points)], 0, (random_tot_color.blue, random_tot_color.green,random_tot_color.red), thickness=cv2.FILLED)

                #contour좌표로 덮은 하의 색
                crop_dir = os.path.join(app.config['CROP_FOLDER'])
                cv2.imwrite(os.path.join(crop_dir,'tit_1.jpg'),crop_1)
                cv2.imwrite(os.path.join(crop_dir,'tit_2.jpg'),crop_2)
                cv2.imwrite(os.path.join(crop_dir,'tit_3.jpg'),crop_3)
                cv2.imwrite(os.path.join(crop_dir,'tot_1.jpg'),crop_4)

                result = cv2.addWeighted(image, 1.2, crop_1, 0.8, 0)



                unique_filename = str(uuid.uuid4()) + '.jpg'

                image_url = unique_filename

                color_data["url"] = image_url

                edit_dir = os.path.join(app.config['EDIT_FOLDER'], unique_filename)
                cv2.imwrite(edit_dir, result)

            elif is_pants_checked == 'true' and is_coat_checked=='false':
                mask = jsonObject['return_object']['person_1']['pants mask']
                pants_color = jsonObject['return_object']['person_1']['pants color']
                pants_colors = [int(x) for x in pants_color.strip('()').split(',')]
                color_data["pants_color"] = (pants_colors[2], pants_colors[1], pants_colors[0])
                pos = jsonObject['return_object']['person_1']['position']

                pants_data["mask"] = mask
                person_position["pos"] = pos


                image = cv2.imread(upload_dir)

                coordinates = mask
                # 문자열에서 공백을 제거하고 "[x, y]" 형식으로 수정
                formatted_coordinates = re.sub(r'\[(\s+)', '[', coordinates)
                formatted_coordinates = re.sub(r'(\s+)\]', ']', formatted_coordinates)
                formatted_coordinates = re.sub(r'\s+', ', ', formatted_coordinates)

                contour_points = [list(map(int, point.strip('[]').split(','))) for point in formatted_coordinates.split('], [')]
                input_string = pos
                # 문자열에서 괄호와 공백을 제거합니다.
                cleaned_string = input_string.replace("[", "").replace("]", "").replace(" ", "")
                # 쉼표로 문자열을 분할하여 숫자를 얻습니다.
                numbers = cleaned_string.split(",")
                # 문자열을 정수로 변환합니다.
                result_array = [int(number) for number in numbers]
                y1 = result_array[0] # 왼쪽 상단 y
                x1 = result_array[1] # 왼쪽 상단 x

                contour_points += np.array([int(x1),int(y1)])

                image_with_contour = image.copy()


                crop_1 = np.zeros_like(image)
                crop_2 = np.zeros_like(image)
                crop_3 = np.zeros_like(image)
                crop_4 = np.zeros_like(image)

                crop_1 = cv2.resize(crop_1,(image.shape[1],image.shape[0]))
                crop_2 = cv2.resize(crop_2,(image.shape[1],image.shape[0]))
                crop_3 = cv2.resize(crop_3,(image.shape[1],image.shape[0]))
                crop_4 = cv2.resize(crop_4,(image.shape[1],image.shape[0]))


                cv2.drawContours(image_with_contour, [np.array(contour_points)], 0, (random_tit_color.blue, random_tit_color.green,random_tit_color.red), thickness=cv2.FILLED)

                cv2.drawContours(crop_1, [np.array(contour_points)], 0, (random_tit_color.blue, random_tit_color.green,random_tit_color.red), thickness=cv2.FILLED)
                cv2.drawContours(crop_2, [np.array(contour_points)], 0, (random_tit_color_1.blue, random_tit_color_1.green,random_tit_color_1.red), thickness=cv2.FILLED)
                cv2.drawContours(crop_3, [np.array(contour_points)], 0, (random_tit_color_2.blue, random_tit_color_2.green,random_tit_color_2.red), thickness=cv2.FILLED)
                cv2.drawContours(crop_4, [np.array(contour_points)], 0, (random_tot_color.blue, random_tot_color.green,random_tot_color.red), thickness=cv2.FILLED)

                #contour좌표로 덮은 하의 색
                crop_dir = os.path.join(app.config['CROP_FOLDER'])
                cv2.imwrite(os.path.join(crop_dir,'tit_1.jpg'),crop_1)
                cv2.imwrite(os.path.join(crop_dir,'tit_2.jpg'),crop_2)
                cv2.imwrite(os.path.join(crop_dir,'tit_3.jpg'),crop_3)
                cv2.imwrite(os.path.join(crop_dir,'tot_1.jpg'),crop_4)

                result = cv2.addWeighted(image, 1.2, crop_1, 0.8, 0)



                unique_filename = str(uuid.uuid4()) + '.jpg'

                image_url = unique_filename

                color_data["url"] = image_url

                edit_dir = os.path.join(app.config['EDIT_FOLDER'], unique_filename)
                cv2.imwrite(edit_dir, result)
            
            
            return {"isUploadSuccess": "success"}
        else:
            return {"isUploadSuccess": "failed"}

@app.route('/get_data', methods=['GET'])
def get_data():

    return jsonify(color_data)

@app.route('/get_image/<path:image_filename>', methods=['GET'])
def get_image(image_filename):
    # 이미지 파일을 클라이언트에 반환합니다.
    image_path = os.path.join(app.config['EDIT_FOLDER'], image_filename)
    if os.path.isfile(image_path):
        return send_file(image_path, mimetype='image/jpg')
    else:
        return "Image not found"
    
@app.route('/', methods=['POST','GET'])
def button_handler():

    
    data = request.get_json()
    button_id = data['button_id']

    # 버튼 ID에 따라 다른 작업 수행
    # 버튼 ID에 따라 다른 작업을 수행합니다.
    if button_id == 'button1':
        # 버튼 1을 눌렀을 때 실행할 작업
        # upload_dir에서 원본 이미지를 로드합니다.
        original_image = cv2.imread(os.path.join(app.config['UPLOAD_FOLDER'], file_name))

        # crop_dir에서 잘린 이미지를 로드합니다.
        cropped_image = cv2.imread(os.path.join(app.config['CROP_FOLDER'], 'tit_1.jpg'))

        # cv2.addWeighted를 사용하여 이미지를 결합합니다.
        alpha = 1.2  # 필요에 따라 알파 값을 조절합니다.
        beta = 0.8 
        result_image = cv2.addWeighted(original_image, alpha, cropped_image, beta, 0.0)

        # 결합된 이미지를 임시 파일에 저장합니다.
        cv2.imwrite('result_image.jpg', result_image)

        # 결합된 이미지를 클라이언트에 반환합니다.
        return send_file('result_image.jpg', mimetype='image/jpg')
    elif button_id == 'button2':
        # 버튼 2를 눌렀을 때 실행할 작업
        # upload_dir에서 원본 이미지를 로드합니다.
        original_image = cv2.imread(os.path.join(app.config['UPLOAD_FOLDER'], file_name))

        # crop_dir에서 잘린 이미지를 로드합니다.
        cropped_image = cv2.imread(os.path.join(app.config['CROP_FOLDER'], 'tit_2.jpg'))

        # cv2.addWeighted를 사용하여 이미지를 결합합니다.
        alpha = 1.2  # 필요에 따라 알파 값을 조절합니다.
        beta = 0.8 
        result_image = cv2.addWeighted(original_image, alpha, cropped_image, beta, 0.0)

        # 결합된 이미지를 임시 파일에 저장합니다.
        cv2.imwrite('result_image.jpg', result_image)

        # 결합된 이미지를 클라이언트에 반환합니다.
        return send_file('result_image.jpg', mimetype='image/jpg')
    elif button_id == 'button3':
        # 버튼 3을 눌렀을 때 실행할 작업
        # upload_dir에서 원본 이미지를 로드합니다.
        original_image = cv2.imread(os.path.join(app.config['UPLOAD_FOLDER'], file_name))

        # crop_dir에서 잘린 이미지를 로드합니다.
        cropped_image = cv2.imread(os.path.join(app.config['CROP_FOLDER'], 'tit_3.jpg'))

        # cv2.addWeighted를 사용하여 이미지를 결합합니다.
        alpha = 1.2  # 필요에 따라 알파 값을 조절합니다.
        beta = 0.8 
        result_image = cv2.addWeighted(original_image, alpha, cropped_image, beta, 0.0)

        # 결합된 이미지를 임시 파일에 저장합니다.
        cv2.imwrite('result_image.jpg', result_image)

        # 결합된 이미지를 클라이언트에 반환합니다.
        return send_file('result_image.jpg', mimetype='image/jpg')
    elif button_id == 'button4':
        # 버튼 3을 눌렀을 때 실행할 작업
        # upload_dir에서 원본 이미지를 로드합니다.
        original_image = cv2.imread(os.path.join(app.config['UPLOAD_FOLDER'], file_name))

        # crop_dir에서 잘린 이미지를 로드합니다.
        cropped_image = cv2.imread(os.path.join(app.config['CROP_FOLDER'], 'tot_1.jpg'))

        # cv2.addWeighted를 사용하여 이미지를 결합합니다.
        alpha = 1.2  # 필요에 따라 알파 값을 조절합니다.
        beta = 0.8 
        result_image = cv2.addWeighted(original_image, alpha, cropped_image, beta, 0.0)

        # 결합된 이미지를 임시 파일에 저장합니다.
        cv2.imwrite('result_image.jpg', result_image)

        # 결합된 이미지를 클라이언트에 반환합니다.
        return send_file('result_image.jpg', mimetype='image/jpg')
    elif button_id == 'button5':
        # 버튼 3을 눌렀을 때 실행할 작업
        # upload_dir에서 원본 이미지를 로드합니다.
        original_image = cv2.imread(os.path.join(app.config['UPLOAD_FOLDER'], file_name))

        cv2.imwrite('result_image.jpg',original_image)

        # 결합된 이미지를 클라이언트에 반환합니다.
        return send_file('result_image.jpg', mimetype='image/jpg')
    else:
        response_data = 'Invalid Button'

    return response_data
    
if __name__ == "__main__":
    app.run(debug=False, host='0.0.0.0', port=80)
