#ifndef __IMAGE_H__
#define __IMAGE_H__

#include <cstdint>
#include <eigen3/Eigen/Dense>
#include <memory>
#include <vector>

using image_t = std::vector<std::vector<uint16_t>>;

class ImageStack {
private:
    uint32_t width;
    uint32_t slices;

    // array3d<Eigen::Vector4f> gradients;

private:
    std::vector<image_t> images;
    // void                 prepare_gradients();
    bool is_inside_world(Eigen::Vector4f world_pos) const;

public:
    ImageStack();
    void add_image(const image_t& img);

    uint32_t get_width() const { return width; }
    void     display(uint32_t slice_index) const;

public:
    uint16_t sample_voxel(float x, float y, float z) const;

    uint16_t sample_color_world(Eigen::Vector4f& world_pos, float cam_distance) const;
    float    sample_opacity_world(Eigen::Vector4f& world_pos) const;

private:
    Eigen::Vector3f world2voxel(Eigen::Vector4f& world_pos) const;

    float y_abs_boundary() const;

public:
    Eigen::Vector4f gradient(Eigen::Vector4f& world_pos) const;
    Eigen::Vector4f normal(Eigen::Vector4f& world_pos) const
    {
        if(gradient(world_pos) == Eigen::Vector4f(0, 0, 0, 0))
            return Eigen::Vector4f(0, 0, 0, 0);
        return gradient(world_pos).normalized();
    }
};

std::shared_ptr<ImageStack> load_ct_image(uint32_t start_index, uint32_t n_slices);

#endif