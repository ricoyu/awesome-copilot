--[[
滑动时间窗口限流算法, 基于zset实现, memeber是一个随机数, score是请求到来的时间点, 毫秒数
每次请求, 传5个参数
1. KEYS[1]    真实的zset键名, 由调用方(Java)拼好传入: slading_window:zset:<业务名>
2. member     一个随机数
3. score      客户端传入的当前的毫秒数, 代表请求的时间点
4. windowSize 时间窗口大小  也是毫秒数, 比如1000, 即从当前时间往前推1000毫秒, 这个时间窗口内有多少个member在
5. limitCount 限流值, 即在指定时间跨度内, 可以通过多少个请求

返回:
true  放行, 允许调用API
false 决绝, 不允许调用

每次放行, 都要把 -inf ~ (score - timeRange) 范围内的member给清除掉, 不然zset会不断膨胀

2026-09-28 集群兼容改造(clauter评审 P0-1): 旧版脚本内用 KEY_PREFIX..name 拼出实际键去读写,
集群下"脚本只能访问 KEYS 声明过的同槽键", 拼接键未声明必被服务端拒绝; 拼接动作移到 Java 调用方,
Redis 中的最终键名不变, 单节点行为不受影响。
--]]

--[[
key        zset key值
minScore   
--]]
local clearOverRanged = function(key, minScore) 
    redis.call("ZREMRANGEBYSCORE", key, "-inf", minScore)
end

--[[
    key        zset 键名(由 Java 拼好: slading_window:zset:<业务名>)
    member     随机数, 没有什么意义, 就是要唯一
    score      客户端请求的时间戳
    windowSize 时间窗口大小
    limitCount 时间窗口内允许通过多少个请求
--]]
local shouldPass = function(key, member, score, windowSize, limitCount) 
    local minScore = score - windowSize
    local members = redis.call("ZRANGE", key, minScore, score, "BYSCORE")
    
    local len=#members
    -- 请求数已经达到限流阈值, 不予放行 
    if #members >= limitCount then
        return false
    else
        -- 请求数还没有达到限流阈值, 先添加新member, 再清理数据, 最后放行
        redis.call("ZADD", key, score, member)
        clearOverRanged(key, minScore)
        return true
    end
end

local key = KEYS[1]
local member = ARGV[1]
local score = tonumber(ARGV[2])
local windowSize = tonumber(ARGV[3])
local limitCount = tonumber(ARGV[4])
return shouldPass(key, member, score, windowSize, limitCount)
