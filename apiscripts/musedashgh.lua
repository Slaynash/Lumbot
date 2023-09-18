local apiData = data:getAsJsonArray()
local mods = {}

for i = 0, apiData:size() - 1, 1 do
    local mod = apiData:get(i)

    table.insert(mods, {
        name = mod:get("Name"):getAsString(),
        version = mod:get("Version"):getAsString(),
        downloadLink = "https://github.com/MDModsDev/ModLinks/raw/main/" .. mod:get("DownloadLink"):getAsString(),
        hash = mod:get("SHA256"):getAsString()
    })
end

return mods