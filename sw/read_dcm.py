import pydicom
import numpy as np
import os

def read_dicom(file_path):
    # Read the DICOM file
    dicom = pydicom.dcmread(file_path)
    return dicom.pixel_array

def normalize_to_8bit(pixel_array):
    # Normalize the pixel array to 8-bit
    pixel_array = pixel_array.astype(np.float32)
    pixel_array -= np.min(pixel_array)
    pixel_array /= np.max(pixel_array)
    pixel_array *= 255.0
    return pixel_array.astype(np.uint8)

def save_as_npy(pixel_array, output_path):
    # Save the pixel array as a .npy file
    np.save(output_path, pixel_array)

def main(input_file, output_file):
    pixel_array = read_dicom(input_file)
    if pixel_array.shape != (512, 512, 512):
        raise ValueError("The DICOM file does not have the required 512x512x512 resolution.")
    pixel_array_8bit = normalize_to_8bit(pixel_array)
    save_as_npy(pixel_array_8bit, output_file)

if __name__ == "__main__":
    input_file = "/path/to/your/dicom/file.dcm"
    output_file = "/path/to/save/8bit_depth_map.npy"
    main(input_file, output_file)