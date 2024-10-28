#ifndef __VECTOR_H__
#define __VECTOR_H__

class Vector3 {
public:
    float x, y, z;

public:
    Vector3();
    Vector3(float x, float y, float z);
    Vector3(const Vector3& v);

    Vector3 operator+(const Vector3& v) const;
    Vector3 operator-(const Vector3& v) const;
    Vector3 operator*(float s) const;
    Vector3 operator/(float s) const;

    Vector3& operator+=(const Vector3& v);
    Vector3& operator-=(const Vector3& v);
    Vector3& operator*=(float s);
    Vector3& operator/=(float s);

public:
    float   dot(const Vector3& v) const;
    Vector3 cross(const Vector3& v) const;
    float   length() const;

    Vector3& normalize();
    Vector3  normalized() const;
};

#endif