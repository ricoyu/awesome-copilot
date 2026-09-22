if redis.call('exists', KEYS[1]) == 0 then
  return -1
end
if redis.call('get', KEYS[1]) == ARGV[1] then
  if redis.call('expire', KEYS[1], ARGV[2]) == 1 then
    return 1
  end
  return -1
end
return 0