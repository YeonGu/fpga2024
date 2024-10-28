import numpy as np


class Vector3:
    def __init__(self, x=0.0, y=0.0, z=0.0):
        self.vector = np.array([x, y, z])

    def __add__(self, other):
        return Vector3(*(self.vector + other.vector))

    def __sub__(self, other):
        return Vector3(*(self.vector - other.vector))

    def __mul__(self, scalar):
        return Vector3(*(self.vector * scalar))

    def dot(self, other):
        return np.dot(self.vector, other.vector)

    def cross(self, other):
        return Vector3(*np.cross(self.vector, other.vector))

    def magnitude(self):
        return np.linalg.norm(self.vector)

    def normalize(self):
        mag = self.magnitude()
        if mag == 0:
            return Vector3()
        return Vector3(*(self.vector / mag))

    def __repr__(self):
        return f"Vector3({self.vector[0]}, {self.vector[1]}, {self.vector[2]})"


class Vector4:
    def __init__(self, x=0.0, y=0.0, z=0.0, w=0.0):
        self.vector = np.array([x, y, z, w])

    def __add__(self, other):
        return Vector4(*(self.vector + other.vector))

    def __sub__(self, other):
        return Vector4(*(self.vector - other.vector))

    def __mul__(self, scalar):
        return Vector4(*(self.vector * scalar))

    def dot(self, other):
        return np.dot(self.vector, other.vector)

    def cross(self, other):
        return Vector4(*np.cross(self.vector, other.vector))

    def magnitude(self):
        return np.linalg.norm(self.vector)

    def normalize(self):
        mag = self.magnitude()
        if mag == 0:
            return Vector4()
        return Vector4(*(self.vector / mag))

    def __repr__(self):
        return f"Vector4({self.vector[0]}, {self.vector[1]}, {self.vector[2]}, {self.vector[3]})"
