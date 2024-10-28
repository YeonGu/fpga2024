from matplotlib import pyplot as plt

from loader import load_png_to_memory


path: str = "/home/kasaki352/workspace/contest/fpga2024/sw/volren/render.png"
data = load_png_to_memory(path)
assert data is not None

plt.imshow(data, cmap="gray")
plt.title("Loaded Image")
plt.show()
