local apiData = data:getAsJsonArray()
local mods = {}

for i = 0, apiData:size() - 1, 1 do
    local mod = apiData:get(i)
    local modDetails = mod:get("Versions"):get(0)

    table.insert(mods, {
        name = mod:get("Name"):getAsString(),
        version = modDetails:get("Version"):getAsString(),
        downloadLink = modDetails:get("DownloadLink"):getAsString(),
        hash = modDetails:get("Hash"):getAsString()
    })
end

return mods