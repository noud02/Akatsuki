# Akatsuki Lua Scripts

Script are made using the language Lua, it runs in a sandbox with limited resources so things like endless while loops aren't possible.

## Libraries

### Discord

#### discord.add_role(member, role)

Returns `nil`

Add a role to a member.

```lua
discord.add_role("yuwui", "awoo")
```

#### discord.remove_role(member, role)

Returns `nil`

Remove a role from a member.

```lua
discord.remove_role("yuwui", "owner")
```

#### discord.get_user(user)

Returns `{ username, discriminator, avatar_url, nick, mention, owner, id }`

Gets info on a user.

```lua
local user = discord.get_user "yuwui"
```

### Context

#### ctx.user

Returns `{ username, discriminator, avatar_url, nick, mention, owner, id }`

```lua
if ctx.user.username == "yuwui" then
    ctx.send "hello master"
end
```

#### ctx.args

Returns `["...","...",...]`

```lua
if ctx.args[1] == "meme" then
    ctx.send "oof"
end
```

#### ctx.send(text)

Returns `nil`

```lua
ctx.send "hello world!"
```
