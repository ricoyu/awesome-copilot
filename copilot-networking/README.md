HTTP, TCP等网络相关操作封装都在这里

# 一 HttpUtils使用示例

1. POST请求

   ```java
   String accountUrl = "http://localhost:8083/account/reduce-balance";
   AccountDTO accountDTO = new AccountDTO();
   accountDTO.setUserId(orderDTO.getUserId());
   accountDTO.setPrice(orderDTO.getMoney());
   
   Result accountResult = HttpUtils.post(accountUrl)
       .body(accountDTO)
       .responseType(Result.class)
       .request();
   ```

   
