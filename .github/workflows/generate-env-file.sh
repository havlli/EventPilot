echo "DISCORD_BOT_TOKEN=${{ secrets.DISCORD_BOT_TOKEN }}" > .env
echo "DATABASE_URL=${{ secrets.DATABASE_URL }}" >> .env
echo "Generated .env file:"
cat .env