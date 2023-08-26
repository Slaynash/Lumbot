local apiData = data:getAsJsonArray()
local mods = {}

for i = 0, apiData:size() - 1, 1 do
    local mod = apiData:get(i)
    local modDetails = mod:get("Version")

    table.insert(mods, {
        name = mod:get("Name"):getAsString(),
        version = modDetails:get("ReadableVersion"):getAsString(),
        downloadLink = modDetails:get("DownloadLink"):getAsString(),
        isbroken = mod:get("IsBroken"):getAsBoolean()
    })
end

return mods
