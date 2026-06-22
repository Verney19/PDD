local stockKey = KEYS[1]
local userKey = KEYS[2]
local userId = ARGV[1]

if redis.call('SISMEMBER', userKey, userId) == 1 then
    redis.call('SREM', userKey, userId)
    return redis.call('INCR', stockKey)
end

return tonumber(redis.call('GET', stockKey))
