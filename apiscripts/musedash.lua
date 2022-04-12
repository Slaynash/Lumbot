local apiData = data:getAsJsonArray()
local mods = {}

for i = 0, apiData:size() - 1, 1 do
    local mod = apiData:get(i)

    table.insert(mods, {
        name = mod:get("name"):getAsString(),
        version = mod:get("version"):getAsString(),
        downloadLink = "https://mdmc.moe/api/v5/download/mod/" .. i,
        isbroken = ~mod:get("functional"):getAsBoolean(),
    })
end

return mods