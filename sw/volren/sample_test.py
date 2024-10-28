from loader import load_ct
import matplotlib.pyplot as plt

if __name__ == "__main__":
    img_array = load_ct()
    print(f"shape of image array: {img_array.shape}")

    # display the 100st image in img_array
    plt.imshow(img_array[99], cmap='gray')
    plt.title("100th Image")
    plt.show()

    