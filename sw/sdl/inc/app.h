#ifndef __APP_H__
#define __APP_H__

#include "cam.h"
#include <SDL2/SDL.h>
#include <SDL2/SDL_image.h>
#include <SDL_events.h>
#include <SDL_keycode.h>
#include <SDL_pixels.h>
#include <SDL_render.h>
#include <SDL_surface.h>
#include <SDL_timer.h>
#include <SDL_video.h>
#include <cstdio>

class SDLApp {
  private:
    SDL_Window *window;
    SDL_Renderer *renderer;
    SDL_Texture *texture;
    bool is_running;
    int window_width, window_height;
    uint8_t *buffer_p;
    SDL_Surface *surface;
    Camera *cam;
    bool is_ctrl_pressed;

  public:
    SDLApp(const char *title, int width, int height);

    ~SDLApp();

    bool load_image(const char *path);

    void handle_event();

    void render() {
        SDL_RenderClear(renderer);

        if (texture) {
            int tex_width, tex_height;
            SDL_QueryTexture(texture, nullptr, nullptr, &tex_width,
                             &tex_height);

            SDL_Rect dstRect;
            dstRect.w = tex_width;
            dstRect.h = tex_height;
            dstRect.x = (window_width - tex_width) / 2;
            dstRect.y = (window_height - tex_height) / 2;

            SDL_RenderCopy(renderer, texture, nullptr, &dstRect);
        }

        SDL_RenderPresent(renderer);
    }

    void run(const char *pathname);
    void set_buffer_ptr(uint8_t *ptr);
    void render_image_from_buffer();
};

#endif // __APP_H__