// Copyright (c) 2020
// Author: Hugo Pointcheval

import 'dart:typed_data';

import 'package:flutter/services.dart';

import 'cipher.dart';

/// Represents a platform, and is usefull to calling
/// methods from a specific platform.
class Platform {
  /// Contains the channel for platform specific code.
  static const MethodChannel _channel = const MethodChannel('native.crypto');

  /// Calls native code.
  static Future<T> call<T>(String method, [Map<String, dynamic> arguments]) {
    return _channel.invokeMethod(method, arguments);
  }

  /// Calls native PBKDF2.
  ///
  /// Takes password and salt as parameters.
  /// And optionnally keyLength in bytes, number of iterations and hash algorithm.
  ///
  /// Returns a key as [Uint8List].
  Future<Uint8List> pbkdf2(
    String password,
    String salt, {
    int keyLength: 32,
    int iteration: 10000,
    String algorithm: 'sha256',
  }) async {
    final Uint8List key = await call('pbkdf2', <String, dynamic>{
      'password': password,
      'salt': salt,
      'keyLength': keyLength,
      'iteration': iteration,
      'algorithm': algorithm,
    });
    return key;
  }

  /// Generates a random key.
  ///
  /// Takes size in bits.
  ///
  /// Returns a key as [Uint8List].
  Future<Uint8List> keygen(int size) async {
    final Uint8List key = await call('keygen', <String, dynamic>{
      'size': size,
    });

    return key;
  }

  /// Generates an RSA key pair.
  ///
  /// Takes size in bits.
  ///
  /// Returns a key pair as list of [Uint8List], the public key is the
  /// first element, and the private is the last.
  Future<List<Uint8List>> rsaKeypairGen(int size) async {
    final List<Uint8List> keypair =
        await call('rsaKeypairGen', <String, dynamic>{
      'size': size,
    });

    return keypair;
  }

  /// Encrypts data with a secret key and algorithm.
  ///
  /// Takes data, key, algorithm, mode and padding as parameters.
  ///
  /// Encrypts data and returns cipher text
  /// and IV as a list of [Uint8List].
  Future<List<Uint8List>> encrypt(
    Uint8List data,
    Uint8List key,
    String algorithm,
    CipherParameters parameters,
  ) async {
    final List<Uint8List> payload = await call('encrypt', <String, dynamic>{
      'data': data,
      'key': key,
      'algorithm': algorithm,
      'mode': parameters.mode.index,
      'padding': parameters.padding.index,
    });
    return payload;
  }

  /// Decrypts a payload with a secret key and algorithm.
  ///
  /// The payload must be a list of `Uint8List`
  /// with encrypted cipher as first and IV as second member.
  Future<Uint8List> decrypt(
    List<Uint8List> payload,
    Uint8List key,
    String algorithm,
    CipherParameters parameters,
  ) async {
    final Uint8List data =
        await _channel.invokeMethod('decrypt', <String, dynamic>{
      'payload': payload,
      'key': key,
      'algorithm': algorithm,
      'mode': parameters.mode.index,
      'padding': parameters.padding.index,
    });
    return data;
  }
}