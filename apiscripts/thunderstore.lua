local apiData = data:getAsJsonArray()
local mods = {}

for i = 0, apiData:size() - 1, 1 do
    local mod = apiData:get(i)
    if mod:get("full_name"):getAsString() == "LavaGang-MelonLoader" then goto continue end

    is_deprecated = modDetails:get("is_deprecated"):getAsBoolean()
    local modDetails = mod:get("versions"):get(0)

    table.insert(mods, {
        name = modDetails:get("name"):getAsString(),
        version = modDetails:get("version_number"):getAsString(),
        downloadLink = modDetails:get("download_url"):getAsString()
    })
    ::continue::
end

return mods