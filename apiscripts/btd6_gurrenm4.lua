local apiData = data:getAsJsonObject():entrySet():iterator()
local mods = {}

while apiData:hasNext() do
    local entry = apiData:next()
    table.insert(mods, {
        name = entry:getKey(),
        version = entry:getValue():get("Version"):getAsString()
    })
end

return mods