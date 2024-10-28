#ifndef __ARRAY_H__
#define __ARRAY_H__
template<typename T> class array3d {
private:
    T*  data;
    int x, y, z;

public:
    array3d() : data(nullptr), x(0), y(0), z(0) { }
    array3d(int x, int y, int z)
    {
        data    = new T[x * y * z];
        this->x = x;
        this->y = y;
        this->z = z;
    }
    ~array3d() { delete[] data; }

    void resize(int x, int y, int z)
    {
        delete[] data;
        data    = new T[x * y * z];
        this->x = x;
        this->y = y;
        this->z = z;
    }

    T& operator()(int x, int y, int z) { return data[x * this->y * this->z + y * this->z + z]; }
    const T& operator()(int x, int y, int z) const
    {
        return data[x * this->y * this->z + y * this->z + z];
    }

    int get_x() const { return x; }
    int get_y() const { return y; }
    int get_z() const { return z; }
};
#endif