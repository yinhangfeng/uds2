function wgs84ToGauss(longitude, latitude) {
  let b; //纬度度数
  let L; //经度度数
  let L0; //中央经线度数
  let L1; //L - L0
  let t; //tanB
  let m; //ltanB
  let N; //卯酉圈曲率半径
  let q2;
  let X; // 高斯平面纵坐标
  let Y; // 高斯平面横坐标
  let s; // 赤道至纬度B的经线弧长
  let f; // 参考椭球体扁率
  let e2; // 椭球第一偏心率
  let a; // 参考椭球体长半轴

  let a1;
  let a2;
  let a3;
  let a4;
  let b1;
  let b2;
  let b3;
  let b4;
  let c0;
  let c1;
  let c2;
  let c3;

  let datum, prjno, zonewide;
  let IPI;

  datum = 84; // 投影基准面类型：北京54基准面为54，西安80基准面为80，WGS84基准面为84
  prjno = 0; // 投影带号
  zonewide = 3;
  IPI = 0.0174532925199433333333; // 3.1415926535898/180.0
  b = latitude; //纬度
  L = longitude; //经度
  if (zonewide == 6) {
    prjno = Math.trunc(L / zonewide) + 1;
    L0 = prjno * zonewide - 3;
  } else {
    prjno = Math.trunc((L - 1.5) / 3) + 1;
    L0 = prjno * 3;
  }
  if (datum == 54) {
    a = 6378245;
    f = 1 / 298.3;
  } else if (datum == 84) {
    a = 6378137;
    f = 1 / 298.257223563;
  }

  L0 = L0 * IPI;
  L = L * IPI;
  b = b * IPI;

  e2 = 2 * f - f * f; // (a*a-b*b)/(a*a);
  L1 = L - L0;
  t = Math.tan(b);
  m = L1 * Math.cos(b);
  N = a / Math.sqrt(1 - e2 * Math.sin(b) * Math.sin(b));
  q2 = (e2 / (1 - e2)) * Math.cos(b) * Math.cos(b);
  a1 =
    1 +
    (3 / 4) * e2 +
    (45 / 64) * e2 * e2 +
    (175 / 256) * e2 * e2 * e2 +
    (11025 / 16384) * e2 * e2 * e2 * e2 +
    (43659 / 65536) * e2 * e2 * e2 * e2 * e2;
  a2 =
    (3 / 4) * e2 +
    (15 / 16) * e2 * e2 +
    (525 / 512) * e2 * e2 * e2 +
    (2205 / 2048) * e2 * e2 * e2 * e2 +
    (72765 / 65536) * e2 * e2 * e2 * e2 * e2;
  a3 =
    (15 / 64) * e2 * e2 +
    (105 / 256) * e2 * e2 * e2 +
    (2205 / 4096) * e2 * e2 * e2 * e2 +
    (10359 / 16384) * e2 * e2 * e2 * e2 * e2;
  a4 =
    (35 / 512) * e2 * e2 * e2 +
    (315 / 2048) * e2 * e2 * e2 * e2 +
    (31185 / 13072) * e2 * e2 * e2 * e2 * e2;
  b1 = a1 * a * (1 - e2);
  b2 = (-1 / 2) * a2 * a * (1 - e2);
  b3 = (1 / 4) * a3 * a * (1 - e2);
  b4 = (-1 / 6) * a4 * a * (1 - e2);
  c0 = b1;
  c1 = 2 * b2 + 4 * b3 + 6 * b4;
  c2 = -(8 * b3 + 32 * b4);
  c3 = 32 * b4;
  s =
    c0 * b +
    Math.cos(b) *
      (c1 * Math.sin(b) +
        c2 * Math.sin(b) * Math.sin(b) * Math.sin(b) +
        c3 * Math.sin(b) * Math.sin(b) * Math.sin(b) * Math.sin(b) * Math.sin(b));
  X =
    s +
    (1 / 2) * N * t * m * m +
    (1 / 24) * (5 - t * t + 9 * q2 + 4 * q2 * q2) * N * t * m * m * m * m +
    (1 / 720) * (61 - 58 * t * t + t * t * t * t) * N * t * m * m * m * m * m * m;
  Y =
    N * m +
    (1 / 6) * (1 - t * t + q2) * N * m * m * m +
    (1 / 120) *
      (5 - 18 * t * t + t * t * t * t - 14 * q2 - 58 * q2 * t * t) *
      N *
      m *
      m *
      m *
      m *
      m;
  Y = Y + 1000000 * prjno + 500000;

  return {
    x: X,
    y: Y,
    z: 0,
  };
}

console.log(wgs84ToGauss(120, 30));
