#include <cmath>
#include <cstdint>
#include <iostream>
#include <vector>

class TextureSampler {
private:
  std::vector<std::vector<std::vector<uint16_t>>> image_stack;
  std::vector<std::vector<std::vector<uint8_t>>> sampled_image;
  const int WIDTH = 512;
  const int HEIGHT = 512;
  const int DEPTH = 512;

public:
  TextureSampler()
      : sampled_image(WIDTH, std::vector<std::vector<uint8_t>>(
                                 HEIGHT, std::vector<uint8_t>(DEPTH, 0))) {}

  float sample(float x, float y, float z) {
    x = x * 234.0f / 512.0f;
    int x0 = std::floor(x);
    int x1 = x0 + 1;
    float dx = x - x0;

    // Ensure we don't access beyond array bounds
    x0 = std::max(0, std::min(x0, static_cast<int>(image_stack.size()) - 1));
    x1 = std::max(0, std::min(x1, static_cast<int>(image_stack.size()) - 1));

    return ((1.0f - dx) * image_stack[x0][y][z] + dx * image_stack[x1][y][z]) /
           2800.0f * 256.0f;
  }

  void process_volume() {
    for (int x = 0; x < WIDTH; ++x) {
      std::cout << "Processing slice " << x << std::endl;
      for (int y = 0; y < HEIGHT; ++y) {
        for (int z = 0; z < DEPTH; ++z) {
          sampled_image[x][y][z] = static_cast<uint8_t>(sample(x, y, z));
        }
      }
    }
  }

  // Getter for the sampled image
  const std::vector<std::vector<std::vector<uint8_t>>> &
  get_sampled_image() const {
    return sampled_image;
  }

  // Method to set the image stack data
  void set_image_stack(
      const std::vector<std::vector<std::vector<uint16_t>>> &stack) {
    image_stack = stack;
  }
};
int main() {
  TextureSampler sampler;
  // TODO: Load your image stack data here

  // Process the volume
  sampler.process_volume();

  // Get the processed image
  const auto &result = sampler.get_sampled_image();

  // TODO: Save or visualize the result

  return 0;
}