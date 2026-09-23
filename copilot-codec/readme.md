# copilot-codec

编解码与安全算法工具集（包名 `com.awesomecopilot.codec`）。对称/非对称加密、哈希签名、Base64、进制转换、JWT 一站式封装。

# 一 能力总览

| 分类 | 核心类 | 提供什么 |
|------|--------|----------|
| 对称加密 | `AesEncryptUtils` | AES 加密/解密（字符串进、字符串出） |
| 对称加密 | `DesEncryptUtils` | DES 加密/解密 + 密钥生成 |
| 非对称加密 | `RsaUtils` | RSA 公钥/私钥加解密、签名验签、PEM 文件加载密钥 |
| 非对称加密 | `Rsa` | 老版单例：首次调用在 `~/private.key`、`~/public.key` 生成并复用密钥对 |
| 哈希/摘要 | `HashUtils` | md5、sha256、hmacSha256（字符串与 int 散列两种输出）、crc16、fnv |
| 哈希/摘要 | `MD5Utils` | MD5 转 int / 转 hex 字符串 |
| Base64 | `Base64Utils` | 编解码多重载 + urlSafe 编解码 |
| 十六进制 | `HexUtils` | hex↔String/int/long/binary、取字节 |
| 进制转换 | `RedixUtils` | int/long/float/byte[]/hex/binary 字符串之间互转（协议开发常用） |
| JWT | `JwtUtils` | jjwt 0.12.x 封装：签发、解析、校验 |

# 二 加密用法

```java
// AES：encrypt/decrypt 成对，key 是口令字符串
String cipher = AesEncryptUtils.encrypt("机密内容", key);
String plain  = AesEncryptUtils.decrypt(cipher, key);
String legacy = AesEncryptUtils.legacyEncrypt(data, key); // 兼容旧密文的旧算法
String newKey = AesEncryptUtils.key();                    // 生成一个新密钥

// RSA（推荐 RsaUtils，从 PEM 文件加载）
RsaUtils rsa = RsaUtils.loadPublicKeyFromPemFile("pub.pem");
String enc = rsa.publicEncrypt(data);      // 公钥加密
String sig = rsa.sign(data);               // 私钥签名（SHA256WithRSA）
boolean ok = rsa.verify(data, sig);

// 哈希
String h1 = HashUtils.sha256(text);
String h2 = HashUtils.md5(file);                        // 文件指纹
String h3 = HashUtils.hmacSha256(message, secret);      // 带密钥
int slot = HashUtils.fnvHash(key);                      // 分桶/一致性散列
```

# 三 JWT（JwtUtils）

密钥派生只有一条路径：把 secretKey 当作 **base64 文本**解码成 HMAC-SHA256 密钥，`createJWT` / `parseJWT` / `generateJwt` 三条 API 生成的 token 互相可解析。

```java
// 签发：expireSecond 为 null 用默认值，负数则不带过期时间
String token = JwtUtils.createJWT(base64Secret, id, subject, 3600L);

// 解析（返回 claims；过期/签名不符抛异常）
Claims claims = JwtUtils.parseJWT(token, base64Secret);

// 老接口：generateJwt 固定用内置 DEFAULT_SECRET 签发，须用单参 parseJWT(token) 解析
String t = JwtUtils.generateJwt(payload);
Date exp = JwtUtils.getExpirationDateFromToken(t, secret);
boolean valid = JwtUtils.validateJwt(token, secret);
```

注意：
- secretKey 必须是 base64 文本；解码是宽松模式，两个"看起来不一样"（如尾部差 1 字符）的口令可能派生出同一把密钥。
- HS256 要求密钥 ≥256 bit，不满足时 `compact()` 抛 `SignatureException`。
- `generateJwt`/`validateJwt` 是老接口，新代码统一用 `createJWT`/`parseJWT`。

# 四 注意事项

- AES/DES 的 ECB/CBC 模式与 IV 策略见类内注释；生产对接第三方时先用 `legacyEncrypt` 确认兼容路径。
- `Rsa` 单例把密钥写在用户主目录，只适合本地开发与演示；跨环境部署用 `RsaUtils` 显式加载密钥文件。
- 协议位运算（网络字节序、报文字段拼拆）优先用 `RedixUtils`，不要手移位。
