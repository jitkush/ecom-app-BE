local currentValue = redis.call("GET", KEYS[1])

if not currentValue then
    return -1
end

if currentValue ~= ARGV[1] then
    return 0
end

redis.call(
    "SET",
    KEYS[1],
    ARGV[2],
    "PX",
    ARGV[3]
)

return 1