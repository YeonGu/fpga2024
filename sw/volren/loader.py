from PIL import Image
import numpy as np
import matplotlib.pyplot as plt


def load_png_to_memory(file_path):
    """
    Load a PNG file into memory as a numpy array. Ensure it is a 16-bit grayscale image.

    :param file_path: Path to the PNG file
    :return: Numpy array of the image if it is 16-bit grayscale, otherwise None
    """
    try:
        with Image.open(file_path) as img:
            # if img.mode != "I;16":
            #     print("Image is not a 16-bit grayscale image")
            #     return None
            img_array = np.array(img)
            return img_array
    except Exception as e:
        print(f"Error loading image: {e}")
        return None


def render() -> np.ndarray:
    render_res = np.zeros((960, 720), dtype=np.uint16)

    return render_res


if __name__ == "__main__":
# def load_ct() -> np.ndarray:
    # file_path = "male_head_ct/1240f.png"
    # image_data = load_png_to_memory(file_path)
    # if image_data is not None:
    #     print("Image loaded successfully")
    #     print(image_data)

    #     # Display the image
    #     plt.imshow(image_data, cmap="gray")
    #     plt.title("Loaded Image")
    #     plt.show()
    # else:
    #     print("Failed to load image")

    image_stack = []

    for i in range(1006, 1240):
        file_path = f"male_head_ct/{i}f.png"
        image_data = load_png_to_memory(file_path)
        if image_data is not None:
            image_stack.append(image_data)
        else:
            print(f"Failed to load image at {file_path}")

    if image_stack:
        image_stack = np.stack(image_stack, axis=0)
        print("All images loaded successfully into a 3D ndarray")
        print(image_stack.shape)
    else:
        print("No images were loaded successfully")
        assert False, "No images loaded"

    plt.imshow(image_stack[220], cmap="gray")
    plt.title("Slice 100 of the 3D Image Stack")
    plt.show()

    # return image_stack
