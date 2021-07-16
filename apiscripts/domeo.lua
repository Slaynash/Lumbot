--[[
[
    {
        "name": "mod name",
        "aliases": ["alias1"],
        "versions": [
            {
                "id": 0,
                "approved": true,
                "name": "version name",
                "modName": "mod name",
                "modId": 0,
                "platforms": ["PC", "QUEST"],
                "dependencies": [
                    {
                        "modName": "mod name",
                        "version": "mod version"
                    }
                ],
                "file": {
                    "hash": "file hash",
                    "url": "file url"
                }
            }
        ],
        "authors": [
            {
                "id": 0,
                "name": "author name",
                "discord": "author discord userid",
                "avatar": "author avatar url"
            }
        ],
        "description": "mod desc",
        "icon": "icon url",
        // platforms
    }
]
]]

local apiData = data:getAsJsonArray()
local mods = {}

for i = 0, apiData:size() - 1, 1 do
    local mod = apiData:get(i)
    local modLatestVersion = mod:get("versions"):get(0)

    local srcAliases = mod:get("aliases")
    local aliases = {}
    for iAlias = 0, srcAliases:size() - 2, 1 do
        table.insert(aliases, srcAliases:get(iAlias):getAsString()) -- lua arrays start at 1
    end

    table.insert(mods, {
        approvalStatus = modLatestVersion:get("approved"):getAsString(),
        name = modLatestVersion:get("name"):getAsString(),
        version = modLatestVersion:get("name"):getAsString(),
        downloadLink = modLatestVersion:get("file"):get("url"):getAsString(),
        aliases = aliases,
        hash = modLatestVersion:get("file"):get("hash"):getAsString()
    })
end

return mods