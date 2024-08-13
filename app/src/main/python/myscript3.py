from PIL import Image
import os
import numpy as np
import io
import cv2
import base64

def get_byte_array(hex_string, length):
    return bytearray.fromhex(hex_string.zfill(length * 2))

def get_finger_general_header(image_size):
    header_array = bytearray(32)
    length_counter = 0

    format_identifier = get_byte_array("46495200", 4)
    header_array[length_counter:length_counter + len(format_identifier)] = format_identifier
    length_counter += len(format_identifier)

    version_number = get_byte_array("30313000", 4)
    header_array[length_counter:length_counter + len(version_number)] = version_number
    length_counter += len(version_number)

    total_size = 32 + 14 + image_size
    record_length = get_byte_array(hex(total_size)[2:], 6)
    header_array[length_counter:length_counter + len(record_length)] = record_length
    length_counter += len(record_length)

    capture_device_id = get_byte_array("00", 2)
    header_array[length_counter:length_counter + len(capture_device_id)] = capture_device_id
    length_counter += len(capture_device_id)

    image_acquisition_level = get_byte_array("1F", 2)
    header_array[length_counter:length_counter + len(image_acquisition_level)] = image_acquisition_level
    length_counter += len(image_acquisition_level)

    number_of_fingers = get_byte_array("1", 1)
    header_array[length_counter:length_counter + len(number_of_fingers)] = number_of_fingers
    length_counter += len(number_of_fingers)

    scale_units = get_byte_array("02", 1)
    header_array[length_counter:length_counter + len(scale_units)] = scale_units
    length_counter += len(scale_units)

    horizontal_scan_resolution = get_byte_array("C5", 2)
    header_array[length_counter:length_counter + len(horizontal_scan_resolution)] = horizontal_scan_resolution
    length_counter += len(horizontal_scan_resolution)

    vertical_scan_resolution = get_byte_array("C5", 2)
    header_array[length_counter:length_counter + len(vertical_scan_resolution)] = vertical_scan_resolution
    length_counter += len(vertical_scan_resolution)

    horizontal_image_resolution = get_byte_array("C5", 2)
    header_array[length_counter:length_counter + len(horizontal_image_resolution)] = horizontal_image_resolution
    length_counter += len(horizontal_image_resolution)

    vertical_image_resolution = get_byte_array("C5", 2)
    header_array[length_counter:length_counter + len(vertical_image_resolution)] = vertical_image_resolution
    length_counter += len(vertical_image_resolution)

    pixel_depth = get_byte_array("08", 1)  # Corrected pixel depth to "08" (8-bit)
    header_array[length_counter:length_counter + len(pixel_depth)] = pixel_depth
    length_counter += len(pixel_depth)

    compression_algorithm = get_byte_array("04", 1)  # Corrected compression algorithm to "04"
    header_array[length_counter:length_counter + len(compression_algorithm)] = compression_algorithm
    length_counter += len(compression_algorithm)

    reserved = get_byte_array("00", 2)
    header_array[length_counter:length_counter + len(reserved)] = reserved
    length_counter += len(reserved)

    return header_array

def get_finger_image_header(image, img_width, img_height):
    total_header_size = 14 + len(image)
    header_array = bytearray(total_header_size)
    length_counter = 0

    total_size = 14 + len(image)
    finger_data_length = get_byte_array(hex(total_size)[2:], 4)
    header_array[length_counter:length_counter + len(finger_data_length)] = finger_data_length
    length_counter += len(finger_data_length)

    finger_or_palm = get_byte_array("00", 1)  # Assuming modality is "finger unknown"
    header_array[length_counter:length_counter + len(finger_or_palm)] = finger_or_palm
    length_counter += len(finger_or_palm)

    view_count = get_byte_array("01", 1)  # Corrected to "01" for view count
    header_array[length_counter:length_counter + len(view_count)] = view_count
    length_counter += len(view_count)

    view_number = get_byte_array("01", 1)  # Corrected to "01" for view number
    header_array[length_counter:length_counter + len(view_number)] = view_number
    length_counter += len(view_number)

    image_quality = get_byte_array("50", 1)  # Corrected image quality
    header_array[length_counter:length_counter + len(image_quality)] = image_quality
    length_counter += len(image_quality)

    impression_type = get_byte_array("00", 1)  # Corrected impression type
    header_array[length_counter:length_counter + len(impression_type)] = impression_type
    length_counter += len(impression_type)

    horizontal_length = get_byte_array(hex(img_width)[2:].zfill(4), 2)
    header_array[length_counter:length_counter + len(horizontal_length)] = horizontal_length
    length_counter += len(horizontal_length)

    vertical_length = get_byte_array(hex(img_height)[2:].zfill(4), 2)
    header_array[length_counter:length_counter + len(vertical_length)] = vertical_length
    length_counter += len(vertical_length)

    reserved = get_byte_array("00", 1)
    header_array[length_counter:length_counter + len(reserved)] = reserved
    length_counter += len(reserved)

    header_array[length_counter:] = image
    return header_array

def convert_image_to_jpeg2000(input_file, output_file = None):
    if not input_file.lower().endswith(('.png', '.jpg', '.jpeg', '.bmp')):
        raise ValueError("Unsupported file format. Please provide a PNG, JPG, JPEG, or BMP file.")
    if isinstance(input_file, str):
        img = cv2.imread(input_file)
    else:
        img = input_file

    if img is None:
        raise ValueError("Failed to read the input image")
    if output_file is None:
        output_file = os.path.splitext(input_file)[0] + '.jp2'
    
    # with Image.open(input_file) as img:
    #     img.save(output_file, format='JPEG2000')

    compression_params = [
        cv2.IMWRITE_JPEG2000_COMPRESSION_X1000, 1000  # Adjust compression level as needed (0-1000)
    ]

    # Save the image in JPEG 2000 format
    success = cv2.imwrite(output_file, img, compression_params)


    print(f'Converted {input_file} to {output_file}')
    return output_file

def add_iso(image_path):
    jpeg2000_path = convert_image_to_jpeg2000(image_path)
    
    with Image.open(jpeg2000_path) as img:
        img_width, img_height = img.size
    
    with open(jpeg2000_path, 'rb') as f:
        img_bytes = f.read()

    general_header = get_finger_general_header(len(img_bytes))
    image_header = get_finger_image_header(img_bytes, img_width, img_height)

    final_image_with_iso = general_header + image_header + img_bytes
    return final_image_with_iso

# def mainly(imgstr, temp_img_path, output_filename):
#     decoded_data = base64.b64decode(imgstr)
#     np_data = np.frombuffer(decoded_data, np.uint8)
#     imge = cv2.imdecode(np_data, cv2.IMREAD_UNCHANGED)
#
#     if imge is None:
#         raise ValueError("Image decoding failed. Please check the provided Base64 image string.")
#
#     cv2.imwrite(temp_img_path, imge)
#
#     final_image_with_iso = add_iso(temp_img_path)
#
#     with open(output_filename, 'wb') as f:
#         f.write(final_image_with_iso)
#
#     return final_image_with_iso
#
# # Example usage
# def main(data,filepath):
#  imgstr = data
#  filename1 = "temp.png"
#  temp_img_path = os.path.join(filepath, filename1)
#  filename2 = "CL_01_LeftHand0_LeftIndex.iso"
#  output_filename = os.path.join(filepath, filename2)
#  result = mainly(imgstr, temp_img_path, output_filename)

def mainly(imgstr, temp_img_path, output_filename):
    try:
        decoded_data = base64.b64decode(imgstr)
        np_data = np.frombuffer(decoded_data, np.uint8)
        imge = cv2.imdecode(np_data, cv2.IMREAD_UNCHANGED)

        if imge is None:
            raise ValueError("Image decoding failed. Please check the provided Base64 image string.")

        print(f"Image shape: {imge.shape}, dtype: {imge.dtype}")

        # Use PIL to save the temporary image
        Image.fromarray(cv2.cvtColor(imge, cv2.COLOR_BGR2RGB)).save(temp_img_path)

        print(f"Temporary image saved successfully: {temp_img_path}")

        final_image_with_iso = add_iso(temp_img_path)

        with open(output_filename, 'wb') as f:
            f.write(final_image_with_iso)

        print(f"Output file saved successfully: {output_filename}")

        return final_image_with_iso
    except Exception as e:
        print(f"An error occurred: {str(e)}")
        raise

def main(data, filepath):
    try:
        print(f"OpenCV version: {cv2.__version__}")

        imgstr = data
        filename1 = "temp.jpg"  # Changed to .jpg
        temp_img_path = os.path.join(filepath, filename1)
        filename2 = "CL_01_LeftHand0_LeftIndex.iso"
        output_filename = os.path.join(filepath, filename2)

        print(f"Temporary image path: {temp_img_path}")
        print(f"Output filename: {output_filename}")

        result = mainly(imgstr, temp_img_path, output_filename)
        print("Processing completed successfully")
        return result
    except Exception as e:
        print(f"An error occurred in main: {str(e)}")
        raise