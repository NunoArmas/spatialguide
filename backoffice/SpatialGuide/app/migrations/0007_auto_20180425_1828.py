# Generated by Django 2.0.3 on 2018-04-25 18:28

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('app', '0006_auto_20180425_1828'),
    ]

    operations = [
        migrations.AlterField(
            model_name='point',
            name='LastUpdate',
            field=models.BigIntegerField(default=1524680893690),
        ),
        migrations.AlterField(
            model_name='route',
            name='LastUpdate',
            field=models.BigIntegerField(default=1524680893690),
        ),
    ]
