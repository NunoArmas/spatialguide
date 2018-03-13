# -*- coding: utf-8 -*-
# Generated by Django 1.11.5 on 2018-03-07 23:20
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('app', '0001_initial'),
    ]

    operations = [
        migrations.CreateModel(
            name='Point',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('name', models.CharField(max_length=50)),
                ('x', models.FloatField()),
                ('y', models.FloatField()),
            ],
        ),
    ]
