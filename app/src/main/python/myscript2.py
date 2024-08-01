import cv2
import numpy as np
from PIL import Image
import base64

def detect_blur_and_bright_spot(image, threshold):
    # Convert image to grayscale
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Apply binary thresholding for bright spot detection
    _, binary_image = cv2.threshold(gray, 200, 255, cv2.THRESH_BINARY)

    # Calculate maximum intensity and variance
    _, max_val, _, _ = cv2.minMaxLoc(gray)
    binary_variance = binary_image.var()
    img_gaussian = cv2.GaussianBlur(gray, (3, 3), 0)
    kernelx = np.array([[1, 1, 1], [0, 0, 0], [-1, -1, -1]])
    kernely = np.array([[-1, 0, 1], [-1, 0, 1], [-1, 0, 1]])
    img_prewittx = cv2.filter2D(img_gaussian, -1, kernelx)
    img_prewitty = cv2.filter2D(img_gaussian, -1, kernely)
    laplacian_variance = img_prewitty.var() + img_prewittx.var()

    # Check blur and bright spot conditions
    if laplacian_variance < threshold:
        return 11
    elif 10500 < binary_variance < 12500:
        return 11
    else:
        return 12

def main(data):
    print("main called")
    decoded_data = base64.b64decode(data)
    np_data = np.frombuffer(decoded_data, np.uint8)
    image = cv2.imdecode(np_data, cv2.IMREAD_UNCHANGED)
    print("image decoded")
    if image is None:
        raise ValueError("Failed to decode image from input data.")

    final = detect_blur_and_bright_spot(image, 675)
    print("final image")
    return final