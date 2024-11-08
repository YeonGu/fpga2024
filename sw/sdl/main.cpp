#include "app.h"

int main(int argc, char **argv) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <image_path>\n", argv[0]);
        return 1;
    }
    SDLApp app("SDLApp", 960, 720);
    app.run(argv[1]);
    return 0;
}
