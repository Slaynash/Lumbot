local apiData = data:getAsJsonArray()
local mods = {}

for i = 0, apiData:size() - 1, 1 do
    local mod = apiData:get(i)
    local modDetails = mod:get("versions"):get(0)

    local srcAliases = mod:get("aliases")
    local aliases = {}
    for iAlias = 0, srcAliases:size() - 2, 1 do
        table.insert(aliases, srcAliases:get(iAlias):getAsString()) -- lua arrays start at 1
    end

    local hash = base64toLowerHexString(modDetails:get("hash"):getAsString())

    table.insert(mods, {
        approvalStatus = modDetails:get("approvalStatus"):getAsString(),
        name = modDetails:get("name"):getAsString(),
        version = modDetails:get("modVersion"):getAsString(),
        downloadLink = modDetails:get("downloadLink"):getAsString(),
        modtype = modDetails:get("modType"):getAsString(),
        haspending = mod:get("hasPending"):getAsBoolean(),
        aliases = aliases,
        hash = hash
    })
end

return mods