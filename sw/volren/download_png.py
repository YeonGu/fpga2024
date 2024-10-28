import os
import requests
from concurrent.futures import ThreadPoolExecutor

# Create the directory if it doesn't exist
os.makedirs("male_head", exist_ok=True)

# Base URL for the images
base_url = "https://data.lhncbc.nlm.nih.gov/public/Visible-Human/Male-Images/PNG_format/radiological/frozenCT/cvm"

def download_image(i):
    file_name = f"{i}f.png"
    url = base_url + file_name
    response = requests.get(url)

    if response.status_code == 200:
        with open(os.path.join("male_head", file_name), "wb") as file:
            file.write(response.content)
        print(f"Downloaded {file_name}")
    else:
        print(f"Failed to download {file_name}")

# Download images from 1006 to 2882 using ThreadPoolExecutor
with ThreadPoolExecutor(max_workers=16) as executor:
    executor.map(download_image, range(1006, 2883))
