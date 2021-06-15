local apiData = data:getAsJsonObject():entrySet():iterator()
local mods = {}

while apiData:hasNext() do
    local entry = apiData:next()

    -- convert the aliases to a LuaTable
    local srcAliases = entry:getValue():get("Aliases")
    local aliases = {}
    for iAlias = 0, srcAliases:size() - 1, 1 do
        table.insert(aliases, srcAliases:get(iAlias):getAsString()) -- lua arrays start at 1
    end

    table.insert(mods, {
        name = entry:getKey(),
        version = entry:getValue():get("Version"):getAsString(),
        downloadLink = entry:getValue():get("Download"):get("browser_download_url"):getAsString(),
        aliases = aliases;
    })
end

return mods