local apiData = data:getAsJsonArray()
local mods = {}

for i = 0, apiData:size() - 1, 1 do
    local mod = apiData:get(i)
    local modDetails = mod:get("versions"):get(0)

    table.insert(mods, {
        name = mod:get("name"):getAsString(),
        version = mod:getValue():get("readableVersion"):getAsString(),
        downloadLink = modDetails:get("downloadlink"):getAsString()
    })
end

return mods