# copilot-validation
一些实用的JSR303 Validation 验证器实现, 自定义了如下验证注解:

1. AllowedValues
2. IP
3. MandatoryIf
4. MandatoryIfs
5. Mobile
6. Password
7. Past
8. UniqueValues
9. UniqueValue
10. Username

## 〇 能力总览

| 注解 | 校验器 | 规则 |
|------|--------|------|
| `@AllowedValues` | AllowedValueValidator | 字段值必须在允许集合内（支持普通类型 + 枚举） |
| `@IP` | IPValidator | 合法 IP 地址（`IPCategory` 枚举区分 v4/v6） |
| `@MandatoryIf` / `@MandatoryIfs` | MandatoryIfValidator | 当 referenceField == referenceValue 时 mandatoryField 必填 |
| `@Mobile` | MobileValidator | 手机号 |
| `@Password` | PasswordValidator | 强密码（大小写、特殊字符、长度等主流安全规则） |
| `@PasswordMatch` | PasswordMatchValidator | 标在类上，两次密码一致 |
| `@Past` | PastValidator | 日期必须是过去时 |
| `@UniqueValue` / `@UniqueValues` | UniqueValueValidator | 字段值在数据库表中唯一（需实现 `UniqueEntityService.count`） |
| `@Username` | UsernameValidator | 字母/数字/下划线/@，以字母或数字开头 |

配套工具：

- `ValidationUtils.validate(obj)`：非 Spring MVC 场景手动触发校验，`getErrorMessage` 提取可读错误。
- `CollectionValidator`：Spring Validator，遍历集合元素逐个校验。
- `SimpleBindingResult`：收集校验错误成 BindingResult 结构（带行号，供 Excel 导入等场景使用）。
- `ValidatePatterns`：`isValidEmail` / `isValidCommaSeparatedEmail` 纯正则判断。

## 1.1 UniqueValue用法

用于校验某个字段值在数据库表中应该值唯一; 比如下面的用法校验品牌名称在表中是唯一的

```java
/**
 * 品牌数据传输对象
 */
@Data
@UniqueValue(table = "pms_brand", primaryKey = "brand_id", property = "name", message = "品牌名称已存在")
public class BrandDTO {

    /**
     * 品牌id
     */
    private Long brandId;

    /**
     * 品牌名
     */
    @NotEmpty(message = "品牌名称不能为空")
    private String name;

    /**
     * 品牌logo地址
     */
    @NotEmpty(message = "品牌logo不能为空")
    @Size(max = 2000, message = "品牌logo长度不能超过2000")
    private String logo;

    /**
     * 介绍
     */
    @NotEmpty(message = "品牌介绍不能为空")
    private String descript;

    /**
     * 显示状态(0-不显示；1-显示)
     */
    @NotNull(message = "显示状态不能为空")
    private Boolean showStatus;

    /**
     * 检索首字母
     */
    @NotBlank(message = "检索首字母不能为空")
    private String firstLetter;

    /**
     * 排序
     */
    @NotNull(message = "排序不能为空")
    private Integer sort;
}
```

* table = "pms_brand"      数据表名
* primaryKey = "brand_id"  如果是下划线的, 则认为是表的主键字段名, 对应bean里面的字段名会自动转成驼峰式
* field = "name"           表中哪个字段是要校验唯一的
* property = "name"        这个bean里面哪个属性是唯一性的, 如果不指定field, 对应的表字段名自动将property转成下划线风格得到
