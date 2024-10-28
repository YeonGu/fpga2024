#include "config.h"
#include <cstdint>
#include <cstdio>
#include <image.h>
#include <memory>
#include <opencv2/opencv.hpp>
#include <stdexcept>
#include <string>
#include <vector>

std::shared_ptr<image_t> load_image(const std::string& filename);

// std::shared_ptr<std::vector<image>> load_ct_image(uint32_t start_index, uint32_t n_slices)
std::shared_ptr<ImageStack> load_ct_image(uint32_t start_index, uint32_t n_slices)
{
    auto filepath_prefix = std::string(CT_FILEPATH);
    auto filename_suffix = std::string("f.png");
    auto image_stack     = std::make_shared<ImageStack>();
    // auto image_stack     = std::make_shared<std::vector<image>>();

    for(uint32_t i = start_index; i < start_index + n_slices; i++) {
        auto filename = filepath_prefix + std::to_string(i) + filename_suffix;
        // image_stack->push_back(*load_image(filename));
        image_stack->add_image(*load_image(filename));
    }

    printf("completed loading %d images, image size %d\n", n_slices, image_stack->get_width());
    return image_stack;
}

std::shared_ptr<image_t> load_image(const std::string& filename)
{
    // use opencv to load image
    cv::Mat image = cv::imread(filename, cv::IMREAD_UNCHANGED);
    if(image.empty())
        throw std::runtime_error("Could not open or find the image: " + filename);
    if(image.rows != image.cols)
        throw std::runtime_error("Image is not square: " + filename);

    auto image_data = std::make_shared<image_t>(image.rows, std::vector<uint16_t>(image.cols));

    for(int i = 0; i < image.rows; ++i) {
        for(int j = 0; j < image.cols; ++j) {
            (*image_data)[i][j] = image.at<uint16_t>(i, j);
        }
    }

    // cv::namedWindow("Loaded Image", cv::WINDOW_AUTOSIZE);
    // cv::imshow("Loaded Image", image);
    // cv::waitKey(0);
    return image_data;
}