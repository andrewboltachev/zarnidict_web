# -*- coding: utf-8 -*-
# Generated by Django 1.9.2 on 2016-05-13 14:57
from __future__ import unicode_literals

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('dicts', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='article',
            name='dictionary',
            field=models.ForeignKey(default=None, on_delete=django.db.models.deletion.CASCADE, to='dicts.Dictionary'),
            preserve_default=False,
        ),
    ]
