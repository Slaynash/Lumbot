local apiData = data:get("data"):getAsJsonArray()
local mods = {}

for i = 0, apiData:size() - 1, 1 do
    local mod = apiData:get(i)
    if mod:get("name"):getAsString() == "MelonLoader" then goto continue end

    local latestFiles = mod:get("latestFiles"):get(0)

    -- CurseForge doesn't have a proper mod versioning, so we have to use the file name instead
    local rawfilename = latestFiles:get("fileName"):getAsString()
    -- remove the file extension
    local filenameWithoutExt = string.gsub(rawfilename, "%.[^%.]*$", "")
    -- get only the version from the filename (can have a "v", and be preceded by a space, "-", or "_")
    local version = string.gsub(filenameWithoutExt, "^(.*?)(v?)[-_ ][^%d]*", "")

    table.insert(mods, {
        name = mod:get("name"):getAsString(),
        version = version,
        downloadLink = latestFiles:get("downloadUrl"):getAsString()
    })
    ::continue::
end

return mods