#include "app.h"
#include "cam.h"
#include <SDL_events.h>
#include <SDL_keycode.h>
#include <SDL_pixels.h>
#include <SDL_render.h>
#include <SDL_surface.h>
#include <SDL_timer.h>
#include <cstdint>
#include "dev.h"

SDLApp::SDLApp(const char *title, int width, int height)
    : window(nullptr), renderer(nullptr), texture(nullptr), is_running(false),
      window_width(width), window_height(height), buffer_p(nullptr),
      surface(nullptr), is_ctrl_pressed(false) {
    if (SDL_Init(SDL_INIT_VIDEO) != 0) {
        fprintf(stderr, "SDL could not initialize! SDL_Error: %s\n",
                SDL_GetError());
        // is_running = false;
        return;
    }

    int img_flags = IMG_INIT_PNG;
    if (!(IMG_Init(img_flags) & img_flags)) {
        fprintf(stderr, "SDL_image could not initialize! SDL_image Error: %s\n",
                IMG_GetError());
        // is_running = false;
        return;
    }

    window = SDL_CreateWindow(title, SDL_WINDOWPOS_UNDEFINED,
                              SDL_WINDOWPOS_UNDEFINED, width, height,
                              SDL_WINDOW_SHOWN);

    if (!window) {
        fprintf(stderr, "Window could not be created! SDL_Error: %s\n",
                SDL_GetError());
        return;
    }

    renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED);
    if (!renderer) {
        fprintf(stderr, "Renderer could not be created! SDL_Error: %s\n",
                SDL_GetError());
        return;
    }

    cam = new Camera(width, height);
}

SDLApp::~SDLApp() {
    if (texture)
        SDL_DestroyTexture(texture);
    if (renderer)
        SDL_DestroyRenderer(renderer);
    if (window)
        SDL_DestroyWindow(window);
    if (surface)
        SDL_FreeSurface(surface);
    if (cam)
        delete cam;
    IMG_Quit();
    SDL_Quit();
}

void SDLApp::set_buffer_ptr(uint8_t *ptr) { buffer_p = ptr; }

void SDLApp::render_image_from_buffer() {
    if (!buffer_p) {
        fprintf(stderr, "Buffer pointer is null!\n");
        return;
    }
    SDL_RenderClear(renderer);
    surface = SDL_CreateRGBSurfaceFrom(
        buffer_p, window_width, window_height, sizeof(uint8_t),
        window_width * sizeof(uint8_t), 0xFF, 0xFF, 0xFF, 0);
    if (!surface) {
        fprintf(stderr, "Unable to create surface from buffer! SDL Error: %s\n",
                SDL_GetError());
        return;
    }

    texture = SDL_CreateTextureFromSurface(renderer, surface);
    SDL_RenderCopy(renderer, texture, NULL, NULL);
    SDL_RenderPresent(renderer);
}

void SDLApp::handle_event() {
    SDL_Event event;
    while (SDL_PollEvent(&event)) {
        switch (event.type) {
        case SDL_QUIT:
            is_running = false;
            break;
        case SDL_KEYDOWN:
            switch (event.key.keysym.sym) {
            case SDLK_ESCAPE:
                is_running = false;
                break;
            case SDLK_LCTRL:
            case SDLK_RCTRL:
                is_ctrl_pressed = true;
                break;
            case SDLK_DOWN:
                if (is_ctrl_pressed) {
                    cam->pitch -= cam->delta_angle;
                } else {
                    cam->pos -= cam->look_at * cam->camera_speed;
                }
                cam->update_mvp_matrix();
                break;
            case SDLK_UP:
                if (is_ctrl_pressed) {
                    cam->pitch += cam->delta_angle;
                } else {
                    cam->pos += cam->look_at * cam->camera_speed;
                }
                cam->update_mvp_matrix();
                break;
            case SDLK_LEFT:
                if (is_ctrl_pressed) {
                    cam->yaw += cam->delta_angle;
                } else {
                    cam->pos += cam->up.cross(cam->look_at).normalized() *
                                cam->camera_speed;
                }
                cam->update_mvp_matrix();
                break;
            case SDLK_RIGHT:
                if (is_ctrl_pressed) {
                    cam->yaw -= cam->delta_angle;
                } else {
                    cam->pos -= cam->up.cross(cam->look_at).normalized() *
                                cam->camera_speed;
                }
                cam->update_mvp_matrix();
                break;
            }
            break;
        case SDL_KEYUP:
            switch (event.key.keysym.sym) {
            case SDLK_LCTRL:
            case SDLK_RCTRL:
                is_ctrl_pressed = false;
                break;
            }
            break;
        }
    }
}

void SDLApp::run(const char *pathname) {
    Dev dev;
    dev.write_base_coord();
    dev.write_mvp_matrix(cam->ortho_proj_mat);
    dev.load_texture(pathname);
    dev.start_render();
    buffer_p = dev.vram_vptr;
    is_running = true;
    while (is_running) {
        handle_event();
        dev.write_mvp_matrix(cam->ortho_proj_mat);
        SDL_Delay(8);
        render_image_from_buffer();
        SDL_Delay(8);
    }
}