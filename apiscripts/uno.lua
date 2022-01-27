local apiData = data:getAsJsonArray()
local mods = {}

for i = 0, apiData:size() - 1, 1 do
    local mod = apiData:get(i)

    table.insert(mods, {
        name = mod:get("Name"):getAsString(),
        version = mod:get("Version"):getAsString(),
        downloadLink = mod:get("DownloadLink"):getAsString(),
        hash = mod:get("Hash"):getAsString()
    })
end

return mods