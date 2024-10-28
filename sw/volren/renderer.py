import dis
from math import dist, pi
import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

from loader import load_ct
from sampler import sample_color, sample_opacity
import threading

"""
Coordinate System:
Y|
 |   /Z
 |  /
 | /
 |/________X
"""

if __name__ == "__main__":
    # camera transform
    cam_pos = np.array([[0], [0], [1]])
    cam_pitch: float = 0  # up / down
    cam_yaw: float = 0  # left / right
    cam_fov: float = 90  # field of view

    tracing_step = 0.005

    bounding_x = 1
    bounding_y = 1
    bounding_z = 1

    # camera transform matrix
    # pitch (vertical) first, then yaw (horizontal) to perform transformation
    pitch_rad = np.radians(cam_pitch)
    yaw_rad = np.radians(cam_yaw)
    cam_mat = (
        np.array(
            # POSITION
            [
                [1, 0, 0, cam_pos[0, 0]],
                [0, 1, 0, cam_pos[1, 0]],
                [0, 0, 1, cam_pos[2, 0]],
                [0, 0, 0, 1],
            ]
        )
        @ np.array(
            # YAW
            [
                [np.cos(yaw_rad), 0, -np.sin(yaw_rad), 0],
                [0, 1, 0, 0],
                [np.sin(yaw_rad), 0, np.cos(yaw_rad), 0],
                [0, 0, 0, 1],
            ]
        )
        @ np.array(
            # PITCH
            [
                [1, 0, 0, 0],
                [0, np.cos(pitch_rad), -np.sin(pitch_rad), 0],
                [0, np.sin(pitch_rad), np.cos(pitch_rad), 0],
                [0, 0, 0, 1],
            ]
        )
    )

    target_resolution = (64, 64)
    max_trace_distance = 3
    bg_color: int = 0

    max_trace_times = int(max_trace_distance / tracing_step)
    disp_ratio = target_resolution[0] / target_resolution[1]

    homo_cam_pos = np.append(cam_pos, [[1]], 0)

    # fig = plt.figure()
    # ax = fig.add_subplot(111, projection="3d")
    # ax.scatter(cam_pos[0], cam_pos[2], cam_pos[1], color="red", label="Camera Position")

    # for head: 1006f-1240f

    render_result = np.zeros((target_resolution[1], target_resolution[0]))
    img = load_ct()

    def trace_ray(x_):
        print(f"tracing ray from row {(x_)}")
        for y_ in range(0, target_resolution[1]):
            # raster space -> NDC Space -> Screen Space
            screen_cor = np.array(
                [
                    [((2 * (x_ + 0.5) / target_resolution[0]) - 1) * disp_ratio],
                    [(1 - (2 * (y_ + 0.5) / target_resolution[1]))],
                ]
            )

            # camera space
            cam_cor = screen_cor * np.tan(np.radians(cam_fov) / 2)
            cam_pix_cor = np.array([[cam_cor[0, 0]], [cam_cor[1, 0]], [-1], [1]])

            # camera space to world space
            pix_world = cam_mat @ cam_pix_cor

            eye_ray_dir = pix_world - homo_cam_pos

            # Plot camera pixel position
            # ax.scatter(
            #     pix_world[0],
            #     pix_world[2],
            #     pix_world[1],
            #     color="blue",
            # )

            # normalize eye ray
            eye_ray_dir_norm = eye_ray_dir / np.linalg.norm(eye_ray_dir)
            # TODO: use bounding box to optimize the performance.

            sample_pos = pix_world
            samples_col = []
            samples_opa = []
            for _ in range(max_trace_times):
                sample_pos = eye_ray_dir_norm * tracing_step + sample_pos
                if sample_opacity(img, sample_pos) == 0:
                    continue
                distance = np.linalg.norm(sample_pos - homo_cam_pos)
                samples_col.append(sample_color(img, sample_pos, distance))
                # if x_ == 0 and y_ == 0:
                # print(f"sample opacity {sample_opacity(img, sample_pos)}")
                samples_opa.append(sample_opacity(img, sample_pos))

            # after sampling, do compositing
            comp_result: float = bg_color
            for color, opacity in zip(reversed(samples_col), reversed(samples_opa)):
                comp_result = color * opacity + comp_result * (1 - opacity)
            render_result[y_, x_] = comp_result

    import concurrent.futures

    with concurrent.futures.ThreadPoolExecutor() as executor:
        futures = [executor.submit(trace_ray, x_) for x_ in range(target_resolution[0])]
        concurrent.futures.wait(futures)

    # ax.scatter(
    #     0, 0, 0, color="black", marker="s", facecolors="none", s=100, label="Origin"
    # )
    # ax.set_xlabel("X")
    # ax.set_ylabel("Z")
    # ax.set_zlabel("Y")
    # ax.legend()

    # show render_result
    plt.imshow(render_result, cmap="gray")
    plt.title("Render Result")
    plt.show()
