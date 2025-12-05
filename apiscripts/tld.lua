local apiData = data:getAsJsonArray()
local mods = {}

for i = 0, apiData:size() - 1, 1 do
    local entry = apiData:get(i)

    -- convert the aliases to a LuaTable
    local srcAliases = entry:get("Aliases")
    local aliases = {}
    for iAlias = 0, srcAliases:size() - 1, 1 do
        table.insert(aliases, srcAliases:get(iAlias):getAsString()) -- lua arrays start at 1
    end

    table.insert(mods, {
        name = entry:get("Name"):getAsString(),
        version = entry:get("Version"):getAsString(),
        downloadLink = entry:get("Download"):getAsString(),
        isbroken = entry:get("Error"):getAsBoolean(),
        modtype = entry:get("Type"):getAsString(),
        aliases = aliases
    })
end

return mods