# AntiPieRay-OG
A fork of [AntiPieRay](https://github.com/orbyfied/AntiPieRay) maintained for 1.19.4 by [TrueOG Network](https://true-og.net). The plugin attempts to prevent usage of the F3 debug pie chart as an exploit for base finding by preventing players from rendering specific block entities from a distance when they are invisible to the player.

### Technical Details
> **Injection**  
> When a player joins, the Netty channel used to communicate  
> with them from the server is injected with a custom packet handler 
> of type `PacketHandler` by the name `AntiPieRay_packet_handler`. 
> The packet handler is configured 
> to listen for writes of `ClientboundBlockEntityDataPacket` by the 
> server. When it is called, it will check if the packet should be cancelled.
>  
> **Check**  
> In order to determine if a packet should be cancelled, a few checks 
> are performed:
> * Check if the block entity type should be checked, this can 
> be specified in the configuration. if this check fails, the packet
> will be let through.
> * Check if the block entities center is within a set distance of the
> player.
> * Check if the block entity is visible to the player utilizing a custom
> ray cast algorithm you can find in `FastRayCast`.

### Change log

1.0: Initial TrueOG Fork based on KiulWasTaken/AntiPieRay

1.0.1: 1.19.4 NMS update by @SKBotNL

1.0.2: 1.19.4 plugin update by @NotAlexNoyle