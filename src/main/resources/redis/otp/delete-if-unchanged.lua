local currentValue = redis.call("GET", KEYS[1])

if not currentValue then
    return -1
end

if currentValue ~= ARGV[1] then
    return 0
end

redis.call("DEL", KEYS[1])

return 1