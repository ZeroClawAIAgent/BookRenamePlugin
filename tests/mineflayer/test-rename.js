import mineflayer from 'mineflayer'

const HOST = process.env.MC_HOST || '127.0.0.1'
const PORT = parseInt(process.env.MC_PORT || '25565')
const USERNAME = 'TestBot'
const TEST_NAME = '<red>Mega Sword'
const EXPECTED_RAW = 'Mega Sword'

let failed = false
function fail(msg) {
  console.error('❌ FAIL:', msg)
  failed = true
}
function ok(msg) {
  console.log('✅', msg)
}

function sleep(ms) {
  return new Promise(r => setTimeout(r, ms))
}

const bot = mineflayer.createBot({
  host: HOST,
  port: PORT,
  username: USERNAME,
  version: '1.21.11',
  auth: 'offline',
})

bot.on('error', err => {
  console.error('Bot error:', err)
  process.exit(1)
})

bot.on('kicked', reason => {
  console.error('Kicked:', reason)
  process.exit(1)
})

bot.on('message', msg => {
  const t = msg.toString()
  if (t.trim()) console.log('[chat]', t)
})

async function waitFor(predicate, timeoutMs = 15000, label = 'condition') {
  const start = Date.now()
  while (Date.now() - start < timeoutMs) {
    try {
      const r = await predicate()
      if (r) return r
    } catch {}
    await sleep(100)
  }
  throw new Error(`Timeout waiting for: ${label}`)
}

bot.once('spawn', async () => {
  try {
    ok(`Spawned as ${USERNAME} on ${HOST}:${PORT}`)

    const hotbar0 = bot.inventory.slots[36]
    if (!hotbar0 || hotbar0.name !== 'stone_sword') {
      fail(`Expected stone_sword in hotbar slot 0, got ${hotbar0 && hotbar0.name}`)
    } else {
      ok('Got stone_sword in hotbar slot 0')
    }

    bot.chat('/minecraft:clear @s')
    bot.chat('/minecraft:give @s minecraft:stone_sword')
    await sleep(500)

    bot.chat('/bookrename')
    await sleep(1000)

    const hand = bot.inventory.slots[36]
    if (!hand || hand.name !== 'writable_book') {
      fail(`Expected writable_book in hand after /bookrename, got ${hand && hand.name}`)
    } else {
      ok('Hand replaced with writable_book')
    }

    await waitFor(
      () => bot.currentWindow && bot.currentWindow.type === 'minecraft:anvil',
      5000,
      'anvil window'
    )
    ok('Anvil window opened')

    await new Promise(resolve => {
      const handler = packet => {
        const data = packet.data || packet
        if (data && (data.field_2555 !== undefined || data.field !== undefined)) {
          bot._client.removeListener('open_screen', handler)
          resolve()
        }
      }
      bot._client.on('open_screen', handler)
      setTimeout(resolve, 800)
    })

    const craftPacketName = 'name_item'
    bot._client.write(craftPacketName, {
      name: TEST_NAME,
    })

    await sleep(500)

    const newHand = bot.inventory.slots[36]
    if (!newHand) {
      fail('No item in hotbar slot 0 after rename')
    } else if (newHand.name !== 'stone_sword') {
      fail(`Expected stone_sword back in hand, got ${newHand.name}`)
    } else if (!newHand.customName) {
      fail(`Stone sword returned but has no customName. NBT: ${JSON.stringify(newHand.nbt || {})}`)
    } else {
      const customStr = JSON.stringify(newHand.customName)
      const hasRed = customStr.toLowerCase().includes('red') || customStr.includes('#ff')
      if (!hasRed) {
        fail(`Custom name found but no red styling detected: ${customStr}`)
      } else {
        ok(`Stone sword renamed with styled customName: ${customStr}`)
      }
    }

    const final = bot.inventory.slots[36]
    if (final && final.customName) {
      const flat = JSON.stringify(final.customName)
      if (flat.includes('Mega Sword')) {
        ok('Renamed item contains expected text "Mega Sword"')
      } else {
        fail(`Renamed item missing expected text: ${flat}`)
      }
    }
  } catch (e) {
    fail(`Exception: ${e.message}`)
    console.error(e)
  } finally {
    bot.quit()
    setTimeout(() => process.exit(failed ? 1 : 0), 500)
  }
})

bot.on('end', () => {
  console.log('Bot disconnected')
})
