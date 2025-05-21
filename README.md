# cdmod
Core idea
•	scary “deep dweller” creature inspired by alien isolation and horror movies such as predator, a quiet place, the thing, it follows, and others
•	generate feelings of tension that the monster is close (but not necessarily an immediate threat)
o	And of being stalked when the monster is near
•	The creature itself has realistic AI inspired by alien isolation
•	make deep caving a tradeoff between loot and risk of encountering the creature

Technical implementation
•	implemented on fabric platform
•	Minecraft version 1.21.4
•	Java 21
Phase 1
•	creature builds awareness of the player in this phase, but cannot spawn. 
•	key variable: alert level
•	Player generated sounds can increase alert level once a player goes below y=40. 
•	Probability and magnitude of alert level increase is driven by intensity of sound and depth (factoring in whether the player is sneaking, which reduces probability of increase)
•	Alert level decays over time, decay accelerates when player is quiet for a while. Decay is slower at deep y levels
•	Monster enters phase 2 once alert level reaches a threshold
•	ambience (custom sounds) trigger at a frequency driven by alert level (with variance)
o	some of the time: cave whistles, whoops, pebbles dropping, distant clicking (or is it a drop of water?)
o	majority of the time: flickering torches (at first it would only be a small flicker, e.g. 2 of 15 light levels, barely noticeable to player, progressing to 11/15)
•	custom sounds triggered by rises in alert level. Intensity driven by alert level, simulating the creature’s distance from the player. Custom sounds are assigned a sound origin (vector or coordinate). To the extent that the player travels towards the sound origin, alert increases faster. To the extent the player travels away, alert increases slower
o	Distant shrieking, e.g.: bennings from the thing, troglodytes from bone tomahawk, annihilation bear,  invasion of the body snatchers scream, layer aztek death whistle or laura palmer)
o	Heavy footsteps travelling along a vector

Phase 2
•	Creature now aware of existence of player (but not precise location) and can now spawn.
•	Spawn
o	never spawns in LOS of player
o	spawns at least 50 blocks away from player
o	only spawns if it would be able to path close to player
o	15% chance to spawn every 2-3 min (only 1 can spawn at a time)
•	Properties
o	Creature is invincible
o	Speed comparable to player, slightly slower
•	Goals
o	Investigates area for player
o	Initial targeting: on spawn, marks approximate location of player (random x/y/z in the 20-30 (driven by intensity of sound that caused it) block-radius sphere around player’s x/y/z, must be an air block)
o	heads toward that location to find player. Uses senses to modify investigation path
•	Senses
o	Hearing 
o	Tracked as perceived intensity (PI) of sound, which equals sound source intensity minus the # of blocks creature is away
	Air blocks counted as ½ a block
o	When PI > 0 
	chance to increase alert level proportionate to PI
	30% chance to trigger active listening – 10 second duration, reduces sound dropoff (effectively increases PI), has a animation / custom audio queue triggered 2 seconds after tick where PI>0
	initiates investigation of the source unless already investigating a higher PI (and if no true awareness/sight)
	Approx area is calculated as a random spot in a sphere around the original sound, monster searches randomly in the area (higher PI = more precise search area)
o	When PI > 10
	Triggers chase
	Sight
o	True vision with a low detection radius (few blocks) in LOS
o	True vision within a larger detection radius (maybe 10-20 blocks) in LOS if PI > some threshold, e.g. 5
o	True vision within a larger detection radius if a nearby player is moving, more so for sprinting
	Smell
o	1/6 of last X blocks player walks on (alternatively the last X blocks the player's hitbox touched) are tracked, in addition to the time it has been since the player was there (with some random variance added - combining to form perceived intensity of scent)
	Coefficient multiplier based on stinky inventory contents
o	Walking on marked blocks triggers an investigation if PI>threshold and it is not investigating something with lower PI
o	“source of smell” creature investigates is considered to be the closest marked block with higher PI such that it follows the trail rather than the player per se
o	Variance needed so it can lose the trail (but should generally tend to push the creature toward player
o	Entering water temporarily reduces scent intensity for 30 seconds


Ambience
o	Audio queues: predator / the grudge clicks (played more slowly than these sounds in their source material - effect should be like the clap from the conjuring or the mouth click from hereditary making the player freeze up)
o	extinguishes light in a wide area of effect (e.g. 40-50 blocks, linear effect) - creates a dimming effect when the creature is walking towards you
o	flickering torches become blackouts
o	flickering/blackouts propagate in a wave from where the creature is
o	freezes water and magma within a certain area of effect
o	screen shakes when it walks, magnitude of shake corresponds to distance from player
o	other mobs run away from creature

Phase 3
•	Creature now knows exactly where the player is
•	Creature begins chase randomly 1-3 seconds after it identifies precise player location
o	Has true awareness of player for X seconds (knows where the player is regardless of whether it can sense the player)
o	True awareness ticks down when player is not detected by senses
o	Applies a multiplier to all senses and speed
o	Prioritizes closest player with true awareness over
•	While chasing
o	Audio cues: Terrifying screeches, heavy footsteps
o	Alert level maxes out
o	Player’s FOV temporarily increases
o	applies darkness status effect to player
o	Prevents the player from placing/breaking blocks
o	Can break blocks on a cooldown
o	Won’t follow players above Y=40
•	Ending chase
o	Duration of chase = 20 seconds from when the creature last located player. If the creature hears or sees player the timer resets
o	When the chase ends, the creature will conduct investigate large area in the last place it knows the player to have been

E.g.s of sound intensity for cumulative sound
 
-block mined - 10
-valuable mined - 15
-block placed - 7
-jump - 3 (on land)
-walk block – 2 (could vary by block type)
-run block - 4
-crouch moving - 1
-shot arrow - 1 for fire, 4 for arrow land (at point of impact - potential to distract monster if it hasn't seen you)
-splash potion break - 20
-explosion - 50 (creepers count as player generating sound)
-broken tool – 20
-fall damage – 15
-door opening – 15
-chest opening – 15
-glass breaking - 20
-enemy creatures (various)
-piston – 25
-extinguishing fire - 15
-crouching reduces noise generated by 50%

