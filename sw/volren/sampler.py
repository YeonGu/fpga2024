from math import dist
from matplotlib.pylab import norm
import numpy as np


"""
See paper "Display of surfaces from volume data" by Marc Levoy.

Sample color from given position. Shading --> Re-sampling

- shading: calculate color of a point. Comes from Phong model and gradient.
- re-sampling: sample color from a point. carry out tri-linear interpolation.
"""

img_pix_size_x = 0.527344  # mm
slice_thickness = 1.0  # mm


def sample_img(img: np.ndarray, world_pos: np.ndarray) -> float:
    # translate world position to correspond voxel position.
    # ASSUME x, z in [-1, 1]^2. y depends on the slices and the thickness of the slice.
    x_boundary = 1
    y_boundary = (img.shape[0] / img.shape[1]) * (slice_thickness / img_pix_size_x)
    z_boundary = 1
    if (
        abs(world_pos[1]) > x_boundary
        or abs(world_pos[0]) > y_boundary
        or abs(world_pos[2]) > z_boundary
    ):
        return 0.0

    # map x from [-1, 1] -> [0, img.shape[0] - 1]
    # map y from [-boundy, boundy] -> [0, img.shape[1] - 1]
    # z from [-1, 1] -> [0, img.shape[2] - 1]
    # FIXME: index overflow (512) here
    xmap = (world_pos[1] + 1) * (img.shape[1]) / 2
    ymap = (world_pos[0] + y_boundary) * (img.shape[0]) / (2 * y_boundary)
    zmap = (world_pos[2] + 1) * (img.shape[2]) / 2
    if (
        abs(xmap + 1) >= img.shape[1]
        or abs(ymap + 1) >= img.shape[0]
        or abs(zmap + 1) >= img.shape[2]
    ):
        return 0.0

    # use trilinear-interpolation to get value at [xmap, ymap, zmap]
    x0, x1 = int(np.floor(xmap)), int(np.ceil(xmap))
    y0, y1 = int(np.floor(ymap)), int(np.ceil(ymap))
    z0, z1 = int(np.floor(zmap)), int(np.ceil(zmap))
    # offset
    xd = (xmap - x0) / (x1 - x0) if x1 != x0 else 0
    yd = (ymap - y0) / (y1 - y0) if y1 != y0 else 0
    zd = (zmap - z0) / (z1 - z0) if z1 != z0 else 0

    c00 = img[y0, x0, z0] * (1 - xd) + img[y1, x0, z0] * xd
    c01 = img[y0, x0, z1] * (1 - xd) + img[y1, x0, z1] * xd
    c10 = img[y0, x1, z0] * (1 - xd) + img[y1, x1, z0] * xd
    c11 = img[y0, x1, z1] * (1 - xd) + img[y1, x1, z1] * xd

    c0 = c00 * (1 - yd) + c10 * yd
    c1 = c01 * (1 - yd) + c11 * yd

    return c0 * (1 - zd) + c1 * zd


def world2voxel(world_pos: np.ndarray, img: np.ndarray) -> np.ndarray:
    # translate world position to correspond voxel position.
    # ASSUME x, z in [-1, 1]^2. y depends on the slices and the thickness of the slice.
    x_boundary = 1
    y_boundary = (img.shape[0] / img.shape[1]) * (slice_thickness / img_pix_size_x)
    z_boundary = 1
    assert (
        abs(world_pos[0]) <= x_boundary
        and abs(world_pos[1]) <= y_boundary
        and abs(world_pos[2]) <= z_boundary
    ), "World position is out of bounds"

    # map x from [-1, 1] -> [0, img.shape[0] - 1]
    # map y from [-boundy, boundy] -> [0, img.shape[1] - 1]
    # z from [-1, 1] -> [0, img.shape[2] - 1]
    xmap = (world_pos[0] + 1) * (img.shape[1]) / 2
    ymap = (world_pos[1] + y_boundary) * (img.shape[0]) / (2 * y_boundary)
    zmap = (world_pos[2] + 1) * (img.shape[2]) / 2

    return np.array([xmap, ymap, zmap])


def sample_color(img: np.ndarray, world_pos: np.ndarray, distance: float) -> float:
    sample = sample_img(img, world_pos)

    # shading
    # consider the diffuse item. TODO: add specular item
    def clamp(min: float, thres: float) -> float:
        return min if min > thres else thres

    # use Phong shading model
    light_source_intensity = 1.0
    ambient_ref_coeff = 0.2 * sample
    diffuse_ref_coeff = 0.8 * sample
    depth_cue_k1 = 1.0
    depth_cue_k2 = 1.0
    light_dir = np.array([0, -1, 0])

    normal = gradient(img, world2voxel(world_pos, img))
    normal = normal / np.linalg.norm(normal)

    sample_res = light_source_intensity * ambient_ref_coeff + (
        light_source_intensity / (depth_cue_k1 + depth_cue_k2 * distance)
    ) * (diffuse_ref_coeff * abs(np.dot(light_dir, normal)))
    return sample_res


def sample_voxel_value(img: np.ndarray, voxel_pos: np.ndarray) -> float:
    assert voxel_pos[0] >= 0 and voxel_pos[0] < img.shape[1]
    assert voxel_pos[1] >= 0 and voxel_pos[1] < img.shape[0]
    assert voxel_pos[2] >= 0 and voxel_pos[2] < img.shape[2]

    x0, x1 = int(np.floor(voxel_pos[0])), int(np.ceil(voxel_pos[0]))
    y0, y1 = int(np.floor(voxel_pos[1])), int(np.ceil(voxel_pos[1]))
    z0, z1 = int(np.floor(voxel_pos[2])), int(np.ceil(voxel_pos[2]))

    xd = (voxel_pos[0] - x0) / (x1 - x0) if x1 != x0 else 0
    yd = (voxel_pos[1] - y0) / (y1 - y0) if y1 != y0 else 0
    zd = (voxel_pos[2] - z0) / (z1 - z0) if z1 != z0 else 0

    c00 = img[y0, x0, z0] * (1 - xd) + img[y1, x0, z0] * xd
    c01 = img[y0, x0, z1] * (1 - xd) + img[y1, x0, z1] * xd
    c10 = img[y0, x1, z0] * (1 - xd) + img[y1, x1, z0] * xd
    c11 = img[y0, x1, z1] * (1 - xd) + img[y1, x1, z1] * xd

    c0 = c00 * (1 - yd) + c10 * yd
    c1 = c01 * (1 - yd) + c11 * yd

    return c0 * (1 - zd) + c1 * zd


def sample_opacity(img: np.ndarray, pos: np.ndarray) -> float:
    sample = sample_img(img, pos)

    # return 0 if out of boundary
    y_boundary = (img.shape[0] / img.shape[1]) * (slice_thickness / img_pix_size_x)
    if abs(pos[0]) > 1 or abs(pos[1]) > y_boundary or abs(pos[2]) > 1:
        return 0.0

    # classification with region boundary surfaces.
    # See paper "Display of surfaces from volume data" by Marc Levoy for more details.
    tissue_val = [30, 100, 900]
    opacity_val = [0, 1.0, 1.0]

    assert len(tissue_val) == len(opacity_val)

    # make linear interpolation with tissue value and opacity value.
    # Sample value & Gradient value => Opacity value
    interpolation = 0.0
    gradient_thres = 3000

    def clamp(val: float, thres: float) -> float:
        return val if val < thres else thres

    for i in range(len(tissue_val) - 1):
        if sample >= tissue_val[i] and sample < tissue_val[i + 1]:
            interpolation = opacity_val[i] + (opacity_val[i + 1] - opacity_val[i]) * (
                sample - tissue_val[i]
            ) / (tissue_val[i + 1] - tissue_val[i])

    return (
        interpolation
        * clamp(np.linalg.norm(gradient(img, world2voxel(pos, img))), gradient_thres)
        / gradient_thres
    )


def gradient(nd: np.ndarray, voxel_pos: np.ndarray) -> np.ndarray:
    # \del f = [ fx, fy, fz ] / 2
    shape = nd.shape
    x, y, z = (
        int(np.floor(voxel_pos[0])),
        int(np.floor(voxel_pos[1])),
        int(np.floor(voxel_pos[2])),
    )

    fx, fy, fz = 0.0, 0.0, 0.0

    def err_pos():
        raise ValueError("Position is out of bounds for gradient calculation")

    # deal with boundary
    if y <= 1 or y >= shape[0] - 2:
        if y <= 1:
            fy = nd[y + 1, x, z] - nd[y, x, z]
        elif y >= shape[0] - 2:
            fy = nd[y, x, z] - nd[y - 1, x, z]
        else:
            print(f"error y {y}")
            err_pos()
    else:
        # fy = (nd[y + 1, x, z] - nd[y - 1, x, z]) / 2.0
        fy = (
            sample_voxel_value(nd, np.array([x, y + 1, z]))
            - sample_voxel_value(nd, np.array([x, y - 1, z]))
        ) / 2.0

    if x <= 1 or x >= shape[1] - 2:
        if x <= 1:
            fx = nd[y, x + 1, z] - nd[y, x, z]
        elif x >= shape[1] - 2:
            fx = nd[y, x, z] - nd[y, x - 1, z]
        else:
            print(f"error x {x}")
            err_pos()
    else:
        fx = (
            sample_voxel_value(nd, np.array([x + 1, y, z]))
            - sample_voxel_value(nd, np.array([x - 1, y, z]))
        ) / 2.0

    if z <= 1 or z >= shape[2] - 2:
        if z <= 1:
            fz = nd[y, x, z + 1] - nd[y, x, z]
        elif z >= shape[2] - 2:
            fz = nd[y, x, z] - nd[y, x, z - 1]
        else:
            print(f"error z {x,y,z}")
            err_pos()
    else:
        fz = (
            sample_voxel_value(nd, np.array([x, y, z + 1]))
            - sample_voxel_value(nd, np.array([x, y, z - 1]))
        ) / 2.0

    # else:
    #     fy = (nd[y + 1, x, z] - nd[y - 1, x, z]) / 2.0
    #     fx = (nd[y, x + 1, z] - nd[y, x - 1, z]) / 2.0
    #     fz = (nd[y, x, z + 1] - nd[y, x, z - 1]) / 2.0

    # return (fx, fy, fz)
    return np.array([fx, fy, fz])
