# Continuity

Continuity is a Fabric mod built around modern APIs to allow for the most efficient connected textures experience possible. It is designed to provide full Optifine parity for all resource packs that use the Optifine/MCPatcher CTM format.

Continuity depends on the Fabric API and is client-side only. With the mod come two built-in resource packs - one provides default connected textures, similar to Optifine, and the other provides a fix for glass pane culling.

## CTM

Nowadays, there are two main CTM (Connected Texture Mapping) formats:

- Optifine/MCPatcher format
- Chisel/ConnectedTexturesMod format

Continuity only supports the Optifine/MCPatcher format. If support for the Chisel format is required, consider using the [ConnectedTexturesMod for Fabric](https://www.curseforge.com/minecraft/mc-mods/ctm-fabric) instead. Note that using Continuity and CTMF at the same time is an unsupported configuration, at least for the time being.

## Setup

1. Make sure you are using the latest version.
2. If you are using Sodium, you must install Indium for the mod to work correctly. It can be downloaded here: https://modrinth.com/mod/indium.
3. Enable the default packs (continuity/default, continuity/glass_pane_culling_fix) in the resource pack menu.
4. Check if you have incompatible mods installed from the list below. Do not remove them yet, but instead update them to their latest version and see if your issue is resolved. If not, then remove them.
    - MoreMCMeta
    - Essential

## Forge Version

I do not see myself working on a Forge version of Continuity for two main reasons:

- Optifine. Optifine has native support for Forge, so a Forge version of Continuity would not be very useful in the first place.
- Forge's model API. It is vastly different from Fabric's model API (FRAPI), which is utilized by the Fabric version, and switching to it would not only require an incredible amount of effort, but also result in the mod being extremely inefficient.

### Links

[CurseForge Page](https://www.curseforge.com/minecraft/mc-mods/continuity)

[Modrinth Page](https://modrinth.com/mod/continuity)

[Discord](https://discord.gg/7rnTYXu)
